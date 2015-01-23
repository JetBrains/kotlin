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

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.types.expressions.LocalClassifierAnalyzer
import org.jetbrains.kotlin.context.GlobalContext
import org.jetbrains.kotlin.resolve.scopes.WritableScope
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.JetClassOrObject
import org.jetbrains.kotlin.types.DynamicTypesSettings
import com.google.common.base.Predicates
import org.jetbrains.kotlin.di.InjectorForLazyLocalClassifierAnalyzer

public class LazyLocalClassifierAnalyzer : LocalClassifierAnalyzer() {
    override fun processClassOrObject(
            globalContext: GlobalContext,
            scope: WritableScope?,
            context: ExpressionTypingContext,
            containingDeclaration: DeclarationDescriptor,
            classOrObject: JetClassOrObject,
            additionalCheckerProvider: AdditionalCheckerProvider,
            dynamicTypesSettings: DynamicTypesSettings
    ) {
        val topDownAnalysisParameters = TopDownAnalysisParameters.create(
                globalContext.storageManager,
                globalContext.exceptionTracker,
                Predicates.equalTo(classOrObject.getContainingFile()), false, true)

        val c = TopDownAnalysisContext(topDownAnalysisParameters)
        c.setOuterDataFlowInfo(context.dataFlowInfo)

        val injector = InjectorForLazyLocalClassifierAnalyzer(
                classOrObject.getProject(),
                globalContext,
                context.trace,
                DescriptorUtils.getContainingModule(containingDeclaration),
                additionalCheckerProvider,
                dynamicTypesSettings
        )

        injector.getLazyTopDownAnalyzer().analyzeDeclarations(
                topDownAnalysisParameters,
                listOf(classOrObject)
        )
    }
}