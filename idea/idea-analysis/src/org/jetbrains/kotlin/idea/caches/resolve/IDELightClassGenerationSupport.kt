/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.containers.ConcurrentFactoryMap
import org.jetbrains.kotlin.asJava.LightClassBuilder
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.asJava.builder.InvalidLightClassDataHolder
import org.jetbrains.kotlin.asJava.builder.LightClassDataHolder
import org.jetbrains.kotlin.asJava.classes.KtUltraLightClass
import org.jetbrains.kotlin.asJava.classes.UltraLightSupport
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.classes.shouldNotBeVisibleAsLightClass
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.codegen.JvmCodegenUtil
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.idea.caches.lightClasses.IDELightClassContexts
import org.jetbrains.kotlin.idea.caches.lightClasses.LazyLightClassDataHolder
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.idea.resolve.frontendService
import org.jetbrains.kotlin.idea.stubindex.KotlinTypeAliasShortNameIndex
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.hasExpectModifier
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperClassifiers
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.lazy.NoDescriptorForDeclarationException
import java.util.concurrent.ConcurrentMap

class IDELightClassGenerationSupport(private val project: Project) : LightClassGenerationSupport() {
    override fun createUltraLightClass(element: KtClassOrObject): KtUltraLightClass? {
        if (element.shouldNotBeVisibleAsLightClass() ||
            element is KtObjectDeclaration && element.isObjectLiteral() ||
            element.isLocal ||
            element is KtEnumEntry
        ) {
            return null
        }

        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return null
        return KtUltraLightClass(element, object : UltraLightSupport {
            override fun isTooComplexForUltraLightGeneration(element: KtClassOrObject): Boolean {
                val facet = KotlinFacet.get(module)
                val pluginClasspath = facet?.configuration?.settings?.compilerArguments?.pluginClasspaths
                if (!pluginClasspath.isNullOrEmpty()) {
                    LOG.debug { "Using heavy light classes for ${element.fqName?.asString()} because of compiler plugins $pluginClasspath" }
                    return true
                }

                val problem = findTooComplexDeclaration(element)
                if (problem != null) {
                    LOG.debug {
                        "Using heavy light classes for ${element.fqName?.asString()} because of ${StringUtil.trimLog(problem.text, 100)}"
                    }
                    return true
                }
                return false
            }

            override val moduleName: String by lazyPub {
                val moduleDescriptor = element.getResolutionFacade().moduleDescriptor
                JvmCodegenUtil.getModuleName(moduleDescriptor)
            }

            override fun findAnnotation(owner: KtAnnotated, fqName: FqName): Pair<KtAnnotationEntry, AnnotationDescriptor>? {
                val candidates = owner.annotationEntries.filter { it.shortName == fqName.shortName() || hasAlias(owner, fqName.shortName()) }
                for (entry in candidates) {
                    val descriptor = analyze(entry).get(BindingContext.ANNOTATION, entry)
                    if (descriptor?.fqName == fqName) {
                        return Pair(entry, descriptor)
                    }
                }
                return null
            }
        })
    }

    private fun findTooComplexDeclaration(declaration: KtDeclaration): PsiElement? {
        fun KtAnnotationEntry.seemsNonTrivial(): Boolean {
            val name = shortName
            return name == null || hasAlias(declaration, name) || name.asString().startsWith("Jvm") && name.asString() != "JvmStatic"
        }

        if (declaration.hasExpectModifier() ||
            declaration.hasModifier(KtTokens.ANNOTATION_KEYWORD) ||
            declaration.hasModifier(KtTokens.INLINE_KEYWORD) && declaration is KtClassOrObject ||
            declaration.hasModifier(KtTokens.DATA_KEYWORD) ||
            declaration.hasModifier(KtTokens.ENUM_KEYWORD) ||
            declaration.hasModifier(KtTokens.SUSPEND_KEYWORD)) {
            return declaration
        }

        declaration.annotationEntries.find(KtAnnotationEntry::seemsNonTrivial)?.let { return it }

        if (declaration is KtClassOrObject) {
            declaration.superTypeListEntries.find { it is KtDelegatedSuperTypeEntry }?.let { return it }

            declaration.primaryConstructor?.let { findTooComplexDeclaration(it) }?.let { return it }

            for (d in declaration.declarations) {
                if (d is KtClassOrObject && !(d is KtObjectDeclaration && d.isCompanion())) continue

                findTooComplexDeclaration(d)?.let { return it }
            }

            if (implementsKotlinCollection(declaration)) {
                return declaration.getSuperTypeList()
            }
        }
        if (declaration is KtCallableDeclaration) {
            declaration.valueParameters.mapNotNull { findTooComplexDeclaration(it) }.firstOrNull()?.let { return it }
            if (declaration.typeReference == null && (declaration as? KtFunction)?.hasBlockBody() != true) {
                findPotentiallyReturnedAnonymousObject(declaration)?.let { return it }
            }
            if (declaration.typeReference?.hasModifier(KtTokens.SUSPEND_KEYWORD) == true) {
                return declaration.typeReference
            }
        }
        if (declaration is KtProperty) {
            declaration.accessors.mapNotNull { findTooComplexDeclaration(it) }.firstOrNull()?.let { return it }
        }

        return null

    }

    private fun findPotentiallyReturnedAnonymousObject(declaration: KtDeclaration): KtObjectDeclaration? {
        val spine = declaration.containingKtFile.stubbedSpine
        for (i in 0 until spine.stubCount) {
            if (spine.getStubType(i) == KtStubElementTypes.OBJECT_DECLARATION) {
                val obj = spine.getStubPsi(i) as KtObjectDeclaration
                if (obj.isObjectLiteral() && PsiTreeUtil.isContextAncestor(declaration, obj, true)) {
                    return obj
                }
            }
        }
        return null
    }

    private fun implementsKotlinCollection(classOrObject: KtClassOrObject): Boolean {
        if (classOrObject.superTypeListEntries.isEmpty()) return false

        return (resolveToDescriptor(classOrObject) as? ClassifierDescriptor)?.getAllSuperClassifiers()?.any {
            it.fqNameSafe.asString().startsWith("kotlin.collections.")
        } == true
    }

    private fun hasAlias(element: KtElement, shortName: Name): Boolean = allAliases(element.containingKtFile)[shortName.asString()] == true

    private fun allAliases(file: KtFile): ConcurrentMap<String, Boolean> = CachedValuesManager.getCachedValue(file) {
        val importAliases = file.importDirectives.mapNotNull { it.aliasName }.toSet()
        val map = ConcurrentFactoryMap.createMap<String, Boolean> { s ->
            s in importAliases || KotlinTypeAliasShortNameIndex.getInstance().get(s, project, file.resolveScope).isNotEmpty()
        }
        CachedValueProvider.Result.create<ConcurrentMap<String, Boolean>>(map, PsiModificationTracker.MODIFICATION_COUNT)
    }

    private val scopeFileComparator = JavaElementFinder.byClasspathComparator(GlobalSearchScope.allScope(project))

    override fun createDataHolderForClass(classOrObject: KtClassOrObject, builder: LightClassBuilder): LightClassDataHolder.ForClass {
        return when {
            classOrObject.shouldNotBeVisibleAsLightClass() -> InvalidLightClassDataHolder
            classOrObject.isLocal -> LazyLightClassDataHolder.ForClass(
                builder,
                exactContextProvider = { IDELightClassContexts.contextForLocalClassOrObject(classOrObject) },
                dummyContextProvider = null,
                diagnosticsHolderProvider = { classOrObject.getDiagnosticsHolder() }
            )
            else -> LazyLightClassDataHolder.ForClass(
                builder,
                exactContextProvider = { IDELightClassContexts.contextForNonLocalClassOrObject(classOrObject) },
                dummyContextProvider = { IDELightClassContexts.lightContextForClassOrObject(classOrObject) },
                diagnosticsHolderProvider = { classOrObject.getDiagnosticsHolder() }
            )
        }
    }


    override fun createDataHolderForFacade(files: Collection<KtFile>, builder: LightClassBuilder): LightClassDataHolder.ForFacade {
        assert(!files.isEmpty()) { "No files in facade" }

        val sortedFiles = files.sortedWith(scopeFileComparator)

        return LazyLightClassDataHolder.ForFacade(
            builder,
            exactContextProvider = { IDELightClassContexts.contextForFacade(sortedFiles) },
            dummyContextProvider = { IDELightClassContexts.lightContextForFacade(sortedFiles) },
            diagnosticsHolderProvider = { files.first().getDiagnosticsHolder() }
        )
    }

    override fun createDataHolderForScript(script: KtScript, builder: LightClassBuilder): LightClassDataHolder.ForScript {
        return LazyLightClassDataHolder.ForScript(
            builder,
            exactContextProvider = { IDELightClassContexts.contextForScript(script) },
            dummyContextProvider = { null },
            diagnosticsHolderProvider = { script.getDiagnosticsHolder() }
        )
    }

    private fun KtElement.getDiagnosticsHolder() =
        getResolutionFacade().frontendService<LazyLightClassDataHolder.DiagnosticsHolder>()

    override fun resolveToDescriptor(declaration: KtDeclaration): DeclarationDescriptor? {
        try {
            return declaration.resolveToDescriptorIfAny(BodyResolveMode.FULL)
        } catch (e: NoDescriptorForDeclarationException) {
            return null
        }
    }

    override fun analyze(element: KtElement) = element.analyze(BodyResolveMode.PARTIAL)

    override fun analyzeWithContent(element: KtClassOrObject) = element.analyzeWithContent()
}

