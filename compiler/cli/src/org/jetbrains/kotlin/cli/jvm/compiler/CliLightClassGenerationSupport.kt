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

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiSearchScopeUtil
import com.intellij.util.Function
import com.intellij.util.SmartList
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.asJava.LightClassBuilder
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.asJava.builder.*
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.asJava.classes.KtLightClassForSourceDeclaration
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer
import org.jetbrains.kotlin.resolve.lazy.ResolveSessionUtils
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.util.slicedMap.ReadOnlySlice
import org.jetbrains.kotlin.util.slicedMap.WritableSlice
import kotlin.properties.Delegates

/**
 * This class solves the problem of interdependency between analyzing Kotlin code and generating JetLightClasses

 * Consider the following example:

 * KClass.kt refers to JClass.java and vice versa

 * To analyze KClass.kt we need to load descriptors from JClass.java, and to do that we need a JetLightClass instance for KClass,
 * which can only be constructed when the structure of KClass is known.

 * To mitigate this, CliLightClassGenerationSupport hold a trace that is shared between the analyzer and JetLightClasses
 */
class CliLightClassGenerationSupport(project: Project) : LightClassGenerationSupport(), CodeAnalyzerInitializer {
    private val psiManager = PsiManager.getInstance(project)
    private var bindingContext: BindingContext by Delegates.notNull()
    private var module: ModuleDescriptor by Delegates.notNull()

    
    override fun initialize(trace: BindingTrace, module: ModuleDescriptor, codeAnalyzer: KotlinCodeAnalyzer) {
        this.bindingContext = trace.bindingContext
        this.module = module

        if (trace !is CliBindingTrace) {
            throw IllegalArgumentException("Shared trace is expected to be subclass of ${CliBindingTrace::class.java.simpleName} class")
        }

        trace.setKotlinCodeAnalyzer(codeAnalyzer)
    }

    override fun createDataHolderForClass(
            classOrObject: KtClassOrObject, builder: LightClassBuilder
    ): LightClassDataHolder.ForClass {
        //force resolve companion for light class generation
        bindingContext.get(BindingContext.CLASS, classOrObject)?.companionObjectDescriptor

        val (stub, bindingContext, diagnostics) = builder(getContext())

        bindingContext.get(BindingContext.CLASS, classOrObject) ?: return InvalidLightClassDataHolder

        return LightClassDataHolderImpl(
                stub,
                diagnostics
        )
    }

    private fun getContext(): LightClassConstructionContext {
        return LightClassConstructionContext(bindingContext, module)
    }

    override fun findClassOrObjectDeclarations(fqName: FqName, searchScope: GlobalSearchScope): Collection<KtClassOrObject> {
        return ResolveSessionUtils.getClassDescriptorsByFqName(module, fqName).mapNotNull {
            val element = DescriptorToSourceUtils.getSourceFromDescriptor(it)
            if (element is KtClassOrObject && PsiSearchScopeUtil.isInScope(searchScope, element)) {
                element
            }
            else null
        }
    }

    override fun findFilesForPackage(fqName: FqName, searchScope: GlobalSearchScope): Collection<KtFile> {
        return bindingContext.get(BindingContext.PACKAGE_TO_FILES, fqName)?.filter {
            PsiSearchScopeUtil.isInScope(searchScope, it)
        } ?: emptyList()
    }

    override fun findClassOrObjectDeclarationsInPackage(
            packageFqName: FqName, searchScope: GlobalSearchScope): Collection<KtClassOrObject> {
        val files = findFilesForPackage(packageFqName, searchScope)
        val result = SmartList<KtClassOrObject>()
        for (file in files) {
            for (declaration in file.declarations) {
                if (declaration is KtClassOrObject) {
                    result.add(declaration)
                }
            }
        }
        return result
    }

    override fun packageExists(fqName: FqName, scope: GlobalSearchScope): Boolean {
        return !module.getPackage(fqName).isEmpty()
    }

    override fun getSubPackages(fqn: FqName, scope: GlobalSearchScope): Collection<FqName> {
        val packageView = module.getPackage(fqn)
        val members = packageView.memberScope.getContributedDescriptors(DescriptorKindFilter.PACKAGES, MemberScope.ALL_NAME_FILTER)
        return ContainerUtil.mapNotNull(members, object : Function<DeclarationDescriptor, FqName> {
            override fun `fun`(member: DeclarationDescriptor): FqName? {
                if (member is PackageViewDescriptor) {
                    return member.fqName
                }
                return null
            }
        })
    }

    override fun getLightClass(classOrObject: KtClassOrObject): KtLightClass? {
        return KtLightClassForSourceDeclaration.create(classOrObject)
    }

    override fun resolveToDescriptor(declaration: KtDeclaration): DeclarationDescriptor? {
        return bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, declaration)
    }

    override fun analyze(element: KtElement) = bindingContext

    override fun analyzeFully(element: KtElement) = bindingContext

    override fun getFacadeClasses(facadeFqName: FqName, scope: GlobalSearchScope): Collection<PsiClass> {
        val filesForFacade = findFilesForFacade(facadeFqName, scope)
        if (filesForFacade.isEmpty()) return emptyList()

        return listOfNotNull<PsiClass>(
                KtLightClassForFacade.createForFacade(psiManager, facadeFqName, scope, filesForFacade))
    }

    override fun getKotlinInternalClasses(fqName: FqName, scope: GlobalSearchScope): Collection<PsiClass> {
        //
        return emptyList()
    }

    override fun findFilesForFacade(facadeFqName: FqName, scope: GlobalSearchScope): Collection<KtFile> {
        if (facadeFqName.isRoot) return emptyList()

        return PackagePartClassUtils.getFilesWithCallables(findFilesForPackage(facadeFqName.parent(), scope)).filter {
            JvmFileClassUtil.getFileClassInfoNoResolve(it).facadeClassFqName == facadeFqName
        }
    }

    override fun createDataHolderForFacade(files: Collection<KtFile>, builder: LightClassBuilder): LightClassDataHolder.ForFacade {
        val (stub, _, diagnostics) = builder(getContext())
        return LightClassDataHolderImpl(stub, diagnostics)
    }

    override fun createTrace(): BindingTraceContext {
        return NoScopeRecordCliBindingTrace()
    }

    // TODO: needs better name + list of keys to skip somewhere
    class NoScopeRecordCliBindingTrace : CliBindingTrace() {
        override fun <K, V> record(slice: WritableSlice<K, V>, key: K, value: V) {
            if (slice === BindingContext.LEXICAL_SCOPE || slice == BindingContext.DATA_FLOW_INFO_BEFORE) {
                // In the compiler there's no need to keep scopes
                return
            }
            super.record(slice, key, value)
        }

        override fun toString(): String {
            return NoScopeRecordCliBindingTrace::class.java.name
        }
    }

    open class CliBindingTrace @TestOnly constructor() : BindingTraceContext() {
        private var kotlinCodeAnalyzer: KotlinCodeAnalyzer? = null

        override fun toString(): String {
            return CliBindingTrace::class.java.name
        }

        fun setKotlinCodeAnalyzer(kotlinCodeAnalyzer: KotlinCodeAnalyzer) {
            this.kotlinCodeAnalyzer = kotlinCodeAnalyzer
        }

        override fun <K, V> get(slice: ReadOnlySlice<K, V>, key: K): V? {
            val value = super.get(slice, key)

            if (value == null) {
                if (BindingContext.FUNCTION === slice || BindingContext.VARIABLE === slice) {
                    if (key is KtDeclaration) {
                        if (!KtPsiUtil.isLocal(key)) {
                            kotlinCodeAnalyzer!!.resolveToDescriptor(key)
                            return super.get<K, V>(slice, key)
                        }
                    }
                }
            }

            return value
        }
    }

    override fun getFacadeClassesInPackage(packageFqName: FqName, scope: GlobalSearchScope): Collection<PsiClass> {
        return PackagePartClassUtils.getFilesWithCallables(findFilesForPackage(packageFqName, scope)).groupBy {
            JvmFileClassUtil.getFileClassInfoNoResolve(it).facadeClassFqName
        }.mapNotNull { KtLightClassForFacade.createForFacade(psiManager, it.key, scope, it.value) }
    }

    override fun getFacadeNames(packageFqName: FqName, scope: GlobalSearchScope): Collection<String> {
        return PackagePartClassUtils.getFilesWithCallables(findFilesForPackage(packageFqName, scope)).map {
            JvmFileClassUtil.getFileClassInfoNoResolve(it).facadeClassFqName.shortName().asString()
        }
    }
}
