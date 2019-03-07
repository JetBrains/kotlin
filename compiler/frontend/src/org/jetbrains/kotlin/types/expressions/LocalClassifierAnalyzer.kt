/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.types.expressions

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.GlobalContext
import org.jetbrains.kotlin.context.withModule
import org.jetbrains.kotlin.context.withProject
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.SupertypeLoopChecker
import org.jetbrains.kotlin.frontend.di.createContainerForLazyLocalClassifierAnalyzer
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.debugText.getDebugText
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension
import org.jetbrains.kotlin.resolve.lazy.*
import org.jetbrains.kotlin.resolve.lazy.data.KtClassInfoUtil
import org.jetbrains.kotlin.resolve.lazy.data.KtClassLikeInfo
import org.jetbrains.kotlin.resolve.lazy.declarations.ClassMemberDeclarationProvider
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory
import org.jetbrains.kotlin.resolve.lazy.declarations.PackageMemberDeclarationProvider
import org.jetbrains.kotlin.resolve.lazy.declarations.PsiBasedClassMemberDeclarationProvider
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassDescriptor
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.LexicalWritableScope
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.WrappedTypeFactory

class LocalClassifierAnalyzer(
    private val globalContext: GlobalContext,
    private val storageManager: StorageManager,
    private val descriptorResolver: DescriptorResolver,
    private val functionDescriptorResolver: FunctionDescriptorResolver,
    private val typeResolver: TypeResolver,
    private val annotationResolver: AnnotationResolver,
    private val platform: TargetPlatform,
    private val compilerServices: PlatformDependentCompilerServices,
    private val lookupTracker: LookupTracker,
    private val supertypeLoopChecker: SupertypeLoopChecker,
    private val languageVersionSettings: LanguageVersionSettings,
    private val delegationFilter: DelegationFilter,
    private val wrappedTypeFactory: WrappedTypeFactory
) {
    fun processClassOrObject(
        scope: LexicalWritableScope?,
        context: ExpressionTypingContext,
        containingDeclaration: DeclarationDescriptor,
        classOrObject: KtClassOrObject
    ) {
        val module = DescriptorUtils.getContainingModule(containingDeclaration)
        val project = classOrObject.project
        val moduleContext = globalContext.withProject(project).withModule(module)
        val container = createContainerForLazyLocalClassifierAnalyzer(
            moduleContext,
            context.trace,
            platform,
            lookupTracker,
            languageVersionSettings,
            context.statementFilter,
            LocalClassDescriptorHolder(
                scope,
                classOrObject,
                containingDeclaration,
                storageManager,
                context,
                module,
                descriptorResolver,
                functionDescriptorResolver,
                typeResolver,
                annotationResolver,
                supertypeLoopChecker,
                languageVersionSettings,
                SyntheticResolveExtension.getInstance(project),
                delegationFilter,
                wrappedTypeFactory
            ),
            compilerServices
        )

        container.get<LazyTopDownAnalyzer>().analyzeDeclarations(
            TopDownAnalysisMode.LocalDeclarations,
            listOf(classOrObject),
            context.dataFlowInfo
        )
    }
}

class LocalClassDescriptorHolder(
    val writableScope: LexicalWritableScope?,
    val myClass: KtClassOrObject,
    val containingDeclaration: DeclarationDescriptor,
    val storageManager: StorageManager,
    val expressionTypingContext: ExpressionTypingContext,
    val moduleDescriptor: ModuleDescriptor,
    val descriptorResolver: DescriptorResolver,
    val functionDescriptorResolver: FunctionDescriptorResolver,
    val typeResolver: TypeResolver,
    val annotationResolver: AnnotationResolver,
    val supertypeLoopChecker: SupertypeLoopChecker,
    val languageVersionSettings: LanguageVersionSettings,
    val syntheticResolveExtension: SyntheticResolveExtension,
    val delegationFilter: DelegationFilter,
    val wrappedTypeFactory: WrappedTypeFactory
) {
    // We do not need to synchronize here, because this code is used strictly from one thread
    private var classDescriptor: ClassDescriptor? = null

    fun isMyClass(element: PsiElement): Boolean = element == myClass
    fun insideMyClass(element: PsiElement): Boolean = PsiTreeUtil.isAncestor(myClass, element, false)

    fun getClassDescriptor(classOrObject: KtClassOrObject, declarationScopeProvider: DeclarationScopeProvider): ClassDescriptor {
        assert(isMyClass(classOrObject)) { "Called on a wrong class: ${classOrObject.getDebugText()}" }
        if (classDescriptor == null) {
            classDescriptor = LazyClassDescriptor(
                object : LazyClassContext {
                    override val declarationScopeProvider = declarationScopeProvider
                    override val storageManager = this@LocalClassDescriptorHolder.storageManager
                    override val trace = expressionTypingContext.trace
                    override val moduleDescriptor = this@LocalClassDescriptorHolder.moduleDescriptor
                    override val descriptorResolver = this@LocalClassDescriptorHolder.descriptorResolver
                    override val functionDescriptorResolver = this@LocalClassDescriptorHolder.functionDescriptorResolver
                    override val typeResolver = this@LocalClassDescriptorHolder.typeResolver
                    override val declarationProviderFactory = object : DeclarationProviderFactory {
                        override fun getClassMemberDeclarationProvider(classLikeInfo: KtClassLikeInfo): ClassMemberDeclarationProvider {
                            return PsiBasedClassMemberDeclarationProvider(storageManager, classLikeInfo)
                        }

                        override fun getPackageMemberDeclarationProvider(packageFqName: FqName): PackageMemberDeclarationProvider? {
                            throw UnsupportedOperationException("Should not be called for top-level declarations")
                        }

                        override fun diagnoseMissingPackageFragment(fqName: FqName, file: KtFile?) {
                            throw UnsupportedOperationException()
                        }
                    }
                    override val annotationResolver = this@LocalClassDescriptorHolder.annotationResolver
                    override val lookupTracker: LookupTracker = LookupTracker.DO_NOTHING
                    override val supertypeLoopChecker = this@LocalClassDescriptorHolder.supertypeLoopChecker
                    override val languageVersionSettings = this@LocalClassDescriptorHolder.languageVersionSettings
                    override val syntheticResolveExtension = this@LocalClassDescriptorHolder.syntheticResolveExtension
                    override val delegationFilter: DelegationFilter = this@LocalClassDescriptorHolder.delegationFilter
                    override val wrappedTypeFactory: WrappedTypeFactory = this@LocalClassDescriptorHolder.wrappedTypeFactory
                },
                containingDeclaration,
                classOrObject.nameAsSafeName,
                KtClassInfoUtil.createClassLikeInfo(classOrObject),
                classOrObject.hasModifier(KtTokens.EXTERNAL_KEYWORD)
            )
            writableScope?.addClassifierDescriptor(classDescriptor!!)
        }

        return classDescriptor!!
    }

    fun getResolutionScopeForClass(classOrObject: KtClassOrObject): LexicalScope {
        assert(isMyClass(classOrObject)) { "Called on a wrong class: ${classOrObject.getDebugText()}" }
        return expressionTypingContext.scope
    }
}

class LocalLazyDeclarationResolver(
    globalContext: GlobalContext,
    trace: BindingTrace,
    private val localClassDescriptorManager: LocalClassDescriptorHolder,
    topLevelDescriptorProvider: TopLevelDescriptorProvider,
    absentDescriptorHandler: AbsentDescriptorHandler
) : LazyDeclarationResolver(globalContext, trace, topLevelDescriptorProvider, absentDescriptorHandler) {

    override fun getClassDescriptor(classOrObject: KtClassOrObject, location: LookupLocation): ClassDescriptor {
        if (localClassDescriptorManager.isMyClass(classOrObject)) {
            return localClassDescriptorManager.getClassDescriptor(classOrObject, scopeProvider)
        }
        return super.getClassDescriptor(classOrObject, location)
    }

    override fun getClassDescriptorIfAny(classOrObject: KtClassOrObject, location: LookupLocation): ClassDescriptor? {
        if (localClassDescriptorManager.isMyClass(classOrObject)) {
            return localClassDescriptorManager.getClassDescriptor(classOrObject, scopeProvider)
        }
        return super.getClassDescriptorIfAny(classOrObject, location)
    }
}


class DeclarationScopeProviderForLocalClassifierAnalyzer(
    lazyDeclarationResolver: LazyDeclarationResolver,
    fileScopeProvider: FileScopeProvider,
    private val localClassDescriptorManager: LocalClassDescriptorHolder
) : DeclarationScopeProviderImpl(lazyDeclarationResolver, fileScopeProvider) {
    override fun getResolutionScopeForDeclaration(elementOfDeclaration: PsiElement): LexicalScope {
        if (localClassDescriptorManager.isMyClass(elementOfDeclaration)) {
            return localClassDescriptorManager.getResolutionScopeForClass(elementOfDeclaration as KtClassOrObject)
        }
        return super.getResolutionScopeForDeclaration(elementOfDeclaration)
    }

    override fun getOuterDataFlowInfoForDeclaration(elementOfDeclaration: PsiElement): DataFlowInfo {
        // nested (non-inner) classes and companion objects are forbidden in local classes, so it's enough to be simply inside the class
        if (localClassDescriptorManager.insideMyClass(elementOfDeclaration)) {
            return localClassDescriptorManager.expressionTypingContext.dataFlowInfo
        }
        return super.getOuterDataFlowInfoForDeclaration(elementOfDeclaration)
    }
}
