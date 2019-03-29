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
import com.intellij.util.containers.ConcurrentFactoryMap
import org.jetbrains.kotlin.asJava.LightClassBuilder
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.asJava.builder.InvalidLightClassDataHolder
import org.jetbrains.kotlin.asJava.builder.LightClassDataHolder
import org.jetbrains.kotlin.asJava.classes.*
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.codegen.ClassBuilderMode
import org.jetbrains.kotlin.codegen.JvmCodegenUtil
import org.jetbrains.kotlin.codegen.state.IncompatibleClassTracker
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.resolve.JvmTarget
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
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperClassifiers
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.lazy.NoDescriptorForDeclarationException
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.KotlinType
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

            override val moduleDescriptor by lazyPub {
                element.getResolutionFacade().moduleDescriptor
            }

            override val moduleName: String by lazyPub {
                JvmCodegenUtil.getModuleName(moduleDescriptor)
            }

            override fun findAnnotation(owner: KtAnnotated, fqName: FqName): Pair<KtAnnotationEntry, AnnotationDescriptor>? {
                val candidates = owner.annotationEntries.filter {
                    it.shortName == fqName.shortName() || owner.containingKtFile.hasAlias(it.shortName)
                }
                for (entry in candidates) {
                    val descriptor = analyze(entry).get(BindingContext.ANNOTATION, entry)
                    if (descriptor?.fqName == fqName) {
                        return Pair(entry, descriptor)
                    }
                }

                if (owner is KtPropertyAccessor) {
                    // We might have from the beginning just resolve the descriptor of the accessor
                    // But we trying to avoid analysis in case property doesn't have any relevant annotations at all
                    // (in case of `findAnnotation` returns null)
                    if (findAnnotation(owner.property, fqName) == null) return null

                    val accessorDescriptor = owner.resolveToDescriptorIfAny() ?: return null

                    // Just reuse the logic of use-site targeted annotation from the compiler
                    val annotationDescriptor = accessorDescriptor.annotations.findAnnotation(fqName) ?: return null
                    val entry = annotationDescriptor.source.getPsi() as? KtAnnotationEntry ?: return null

                    return entry to annotationDescriptor
                }

                return null
            }

            override val deprecationResolver: DeprecationResolver by lazyPub {
                element.getResolutionFacade().getFrontendService(DeprecationResolver::class.java)
            }

            override val typeMapper: KotlinTypeMapper by lazyPub {
                KotlinTypeMapper(
                    BindingContext.EMPTY, ClassBuilderMode.LIGHT_CLASSES,
                    moduleName, KotlinTypeMapper.LANGUAGE_VERSION_SETTINGS_DEFAULT, // TODO use proper LanguageVersionSettings
                    jvmTarget = JvmTarget.JVM_1_8,
                    typePreprocessor = KotlinType::cleanFromAnonymousTypes
                )
            }
        })
    }

    private fun findTooComplexDeclaration(declaration: KtDeclaration): PsiElement? {
        if (declaration.hasExpectModifier() ||
            declaration.hasModifier(KtTokens.ANNOTATION_KEYWORD) ||
            declaration.hasModifier(KtTokens.INLINE_KEYWORD) && declaration is KtClassOrObject ||
            declaration.hasModifier(KtTokens.SUSPEND_KEYWORD)
        ) {
            return declaration
        }


        if (declaration is KtClassOrObject) {
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
            if (declaration.typeReference?.hasModifier(KtTokens.SUSPEND_KEYWORD) == true) {
                return declaration.typeReference
            }
        }
        if (declaration is KtProperty) {
            declaration.accessors.mapNotNull { findTooComplexDeclaration(it) }.firstOrNull()?.let { return it }
        }

        return null

    }

    private fun implementsKotlinCollection(classOrObject: KtClassOrObject): Boolean {
        if (classOrObject.superTypeListEntries.isEmpty()) return false

        return (resolveToDescriptor(classOrObject) as? ClassifierDescriptor)?.getAllSuperClassifiers()?.any {
            it.fqNameSafe.asString().startsWith("kotlin.collections.")
        } == true
    }

    private fun KtFile.hasAlias(shortName: Name?): Boolean {
        if (shortName == null) return false
        return allAliases(this)[shortName.asString()] == true
    }

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

