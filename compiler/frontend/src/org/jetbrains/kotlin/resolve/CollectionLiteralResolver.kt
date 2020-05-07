/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.incremental.KotlinLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtCollectionLiteralExpression
import org.jetbrains.kotlin.resolve.BindingContext.COLLECTION_LITERAL_CALL
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.util.CallMaker
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils.NO_EXPECTED_TYPE
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext
import org.jetbrains.kotlin.types.expressions.KotlinTypeInfo
import org.jetbrains.kotlin.types.expressions.typeInfoFactory.createTypeInfo
import org.jetbrains.kotlin.types.expressions.typeInfoFactory.noTypeInfo

class CollectionLiteralResolver(
    val module: ModuleDescriptor,
    val callResolver: CallResolver,
    val languageVersionSettings: LanguageVersionSettings
) {
    fun resolveCollectionLiteral(
        collectionLiteralExpression: KtCollectionLiteralExpression,
        context: ExpressionTypingContext
    ): KotlinTypeInfo {
        if (!isInsideAnnotationEntryOrClass(collectionLiteralExpression)) {
            context.trace.report(UNSUPPORTED.on(collectionLiteralExpression, "Collection literals outside of annotations"))
        }

        checkSupportsArrayLiterals(collectionLiteralExpression, context)

        return resolveCollectionLiteralSpecialMethod(collectionLiteralExpression, context)
    }

    private fun resolveCollectionLiteralSpecialMethod(
        expression: KtCollectionLiteralExpression,
        context: ExpressionTypingContext
    ): KotlinTypeInfo {
        val call = CallMaker.makeCallForCollectionLiteral(expression)
        val callName = getArrayFunctionCallName(context.expectedType)
        val functionDescriptors = getFunctionDescriptorForCollectionLiteral(expression, callName)
        if (functionDescriptors.isEmpty()) {
            context.trace.report(
                MISSING_STDLIB.on(
                    expression, "Collection literal call '$callName()' is unresolved"
                )
            )
            return noTypeInfo(context)
        }

        val resolutionResults = callResolver.resolveCollectionLiteralCallWithGivenDescriptor(context, expression, call, functionDescriptors)

        if (!resolutionResults.isSingleResult) {
            return noTypeInfo(context)
        }

        context.trace.record(COLLECTION_LITERAL_CALL, expression, resolutionResults.resultingCall)
        return createTypeInfo(resolutionResults.resultingDescriptor.returnType, context)
    }

    private fun getFunctionDescriptorForCollectionLiteral(
        expression: KtCollectionLiteralExpression,
        callName: Name
    ): Collection<SimpleFunctionDescriptor> {
        val memberScopeOfKotlinPackage = module.getPackage(KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME).memberScope
        return memberScopeOfKotlinPackage.getContributedFunctions(callName, KotlinLookupLocation(expression))
    }

    private fun checkSupportsArrayLiterals(expression: KtCollectionLiteralExpression, context: ExpressionTypingContext) {
        if (isInsideAnnotationEntryOrClass(expression) &&
            !languageVersionSettings.supportsFeature(LanguageFeature.ArrayLiteralsInAnnotations)) {
            context.trace.report(UNSUPPORTED_FEATURE.on(expression, LanguageFeature.ArrayLiteralsInAnnotations to languageVersionSettings))
        }
    }

    private fun isInsideAnnotationEntryOrClass(expression: KtCollectionLiteralExpression): Boolean {
        val parent = PsiTreeUtil.getParentOfType(expression, KtAnnotationEntry::class.java, KtClass::class.java)
        return parent is KtAnnotationEntry || (parent is KtClass && parent.isAnnotation())
    }

    private fun getArrayFunctionCallName(expectedType: KotlinType): Name {
        if (NO_EXPECTED_TYPE === expectedType || !KotlinBuiltIns.isPrimitiveArray(expectedType)) {
            return ArrayFqNames.ARRAY_OF_FUNCTION
        }

        val descriptor = expectedType.constructor.declarationDescriptor ?: return ArrayFqNames.ARRAY_OF_FUNCTION

        return ArrayFqNames.PRIMITIVE_TYPE_TO_ARRAY[KotlinBuiltIns.getPrimitiveArrayType(descriptor)] ?: ArrayFqNames.ARRAY_OF_FUNCTION
    }
}
