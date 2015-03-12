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

import org.jetbrains.kotlin.context.GlobalContext
import org.jetbrains.kotlin.resolve.scopes.WritableScope
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.JetClassOrObject
import org.jetbrains.kotlin.types.DynamicTypesSettings
import com.google.common.base.Predicates
import org.jetbrains.kotlin.di.InjectorForLazyLocalClassifierAnalyzer
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.resolve.lazy.DeclarationScopeProvider
import org.jetbrains.kotlin.psi.debugText.getDebugText
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassDescriptor
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory
import org.jetbrains.kotlin.resolve.lazy.data.JetClassLikeInfo
import org.jetbrains.kotlin.resolve.lazy.declarations.ClassMemberDeclarationProvider
import org.jetbrains.kotlin.resolve.lazy.declarations.PsiBasedClassMemberDeclarationProvider
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.lazy.declarations.PackageMemberDeclarationProvider
import org.jetbrains.kotlin.psi.psiUtil.isObjectLiteral
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.resolve.lazy.data.JetClassInfoUtil
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.resolve.lazy.LazyDeclarationResolver
import org.jetbrains.kotlin.resolve.lazy.DeclarationScopeProviderImpl
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.lazy.LazyClassContext

public class LocalClassifierAnalyzer(
        val descriptorResolver: DescriptorResolver,
        val funcionDescriptorResolver: FunctionDescriptorResolver,
        val typeResolver: TypeResolver,
        val annotationResolver: AnnotationResolver
) {
    fun processClassOrObject(
            globalContext: GlobalContext,
            scope: WritableScope?,
            context: ExpressionTypingContext,
            containingDeclaration: DeclarationDescriptor,
            classOrObject: JetClassOrObject,
            additionalCheckerProvider: AdditionalCheckerProvider,
            dynamicTypesSettings: DynamicTypesSettings
    ) {
        val topDownAnalysisParameters = TopDownAnalysisParameters.create(globalContext.storageManager, globalContext.exceptionTracker, false, true)

        val moduleDescriptor = DescriptorUtils.getContainingModule(containingDeclaration)
        val injector = InjectorForLazyLocalClassifierAnalyzer(
                classOrObject.getProject(),
                globalContext,
                context.trace,
                moduleDescriptor,
                additionalCheckerProvider,
                dynamicTypesSettings,
                LocalClassDescriptorHolder(
                        scope,
                        classOrObject,
                        containingDeclaration,
                        globalContext.storageManager,
                        context,
                        moduleDescriptor,
                        descriptorResolver,
                        funcionDescriptorResolver,
                        typeResolver,
                        annotationResolver
                )
        )

        injector.getLazyTopDownAnalyzer().analyzeDeclarations(
                topDownAnalysisParameters,
                listOf(classOrObject),
                context.dataFlowInfo
        )
    }
}

class LocalClassDescriptorHolder(
        val writableScope: WritableScope?,
        val myClass: JetClassOrObject,
        val containingDeclaration: DeclarationDescriptor,
        val storageManager: StorageManager,
        val expressionTypingContext: ExpressionTypingContext,
        val moduleDescriptor: ModuleDescriptor,
        val descriptorResolver: DescriptorResolver,
        val functionDescriptorResolver: FunctionDescriptorResolver,
        val typeResolver: TypeResolver,
        val annotationResolver: AnnotationResolver
) {
    // We do not need to synchronize here, because this code is used strictly from one thread
    private var classDescriptor: ClassDescriptor? = null

    fun isMyClass(element: PsiElement): Boolean = element == myClass
    fun insideMyClass(element: PsiElement): Boolean = PsiTreeUtil.isAncestor(myClass, element, false)

    fun getClassDescriptor(classOrObject: JetClassOrObject, declarationScopeProvider: DeclarationScopeProvider): ClassDescriptor {
        assert(isMyClass(classOrObject)) {"Called on a wrong class: ${classOrObject.getDebugText()}"}
        if (classDescriptor == null) {
            classDescriptor = LazyClassDescriptor(
                    object : LazyClassContext {
                        override val scopeProvider = declarationScopeProvider
                        override val storageManager = this@LocalClassDescriptorHolder.storageManager
                        override val trace = expressionTypingContext.trace
                        override val moduleDescriptor = this@LocalClassDescriptorHolder.moduleDescriptor
                        override val descriptorResolver = this@LocalClassDescriptorHolder.descriptorResolver
                        override val functionDescriptorResolver = this@LocalClassDescriptorHolder.functionDescriptorResolver
                        override val typeResolver = this@LocalClassDescriptorHolder.typeResolver
                        override val declarationProviderFactory = object : DeclarationProviderFactory {
                            override fun getClassMemberDeclarationProvider(classLikeInfo: JetClassLikeInfo): ClassMemberDeclarationProvider {
                                return PsiBasedClassMemberDeclarationProvider(storageManager, classLikeInfo)
                            }

                            override fun getPackageMemberDeclarationProvider(packageFqName: FqName): PackageMemberDeclarationProvider? {
                                throw UnsupportedOperationException("Should not be called for top-level declarations")
                            }

                        }
                        override val annotationResolver = this@LocalClassDescriptorHolder.annotationResolver
                    }
                    ,
                    containingDeclaration,
                    classOrObject.getNameAsSafeName(),
                    JetClassInfoUtil.createClassLikeInfo(classOrObject)
            )
            writableScope?.addClassifierDescriptor(classDescriptor!!)
        }

        return classDescriptor!!
    }

    fun getResolutionScopeForClass(classOrObject: JetClassOrObject): JetScope {
        assert (isMyClass(classOrObject)) {"Called on a wrong class: ${classOrObject.getDebugText()}"}
        return expressionTypingContext.scope
    }
}

class LocalLazyDeclarationResolver(
        globalContext: GlobalContext,
        trace: BindingTrace,
        val localClassDescriptorManager: LocalClassDescriptorHolder
) : LazyDeclarationResolver(globalContext, trace) {

    override fun getClassDescriptor(classOrObject: JetClassOrObject): ClassDescriptor {
        if (localClassDescriptorManager.isMyClass(classOrObject)) {
            return localClassDescriptorManager.getClassDescriptor(classOrObject, scopeProvider)
        }
        return super.getClassDescriptor(classOrObject)
    }
}


class DeclarationScopeProviderForLocalClassifierAnalyzer(
        lazyDeclarationResolver: LazyDeclarationResolver,
        val localClassDescriptorManager: LocalClassDescriptorHolder
) : DeclarationScopeProviderImpl(lazyDeclarationResolver) {
    override fun getResolutionScopeForDeclaration(elementOfDeclaration: PsiElement): JetScope {
        if (localClassDescriptorManager.isMyClass(elementOfDeclaration)) {
            return localClassDescriptorManager.getResolutionScopeForClass(elementOfDeclaration as JetClassOrObject)
        }
        return super.getResolutionScopeForDeclaration(elementOfDeclaration)
    }

    override fun getOuterDataFlowInfoForDeclaration(elementOfDeclaration: PsiElement): DataFlowInfo {
        // nested (non-inner) classes and default objects are forbidden in local classes, so it's enough to be simply inside the class
        if (localClassDescriptorManager.insideMyClass(elementOfDeclaration)) {
            return localClassDescriptorManager.expressionTypingContext.dataFlowInfo
        }
        return super.getOuterDataFlowInfoForDeclaration(elementOfDeclaration)
    }
}