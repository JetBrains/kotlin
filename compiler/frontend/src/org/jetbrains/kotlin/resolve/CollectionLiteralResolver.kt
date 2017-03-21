/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.diagnostics.Errors.UNSUPPORTED
import org.jetbrains.kotlin.diagnostics.Errors.UNSUPPORTED_FEATURE
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

object CollectionLiteralResolver {
    private val PRIMITIVE_TYPE_TO_ARRAY: Map<PrimitiveType, Name> = hashMapOf(
            PrimitiveType.BOOLEAN to Name.identifier("booleanArrayOf"),
            PrimitiveType.CHAR to Name.identifier("charArrayOf"),
            PrimitiveType.INT to Name.identifier("intArrayOf"),
            PrimitiveType.BYTE to Name.identifier("byteArrayOf"),
            PrimitiveType.SHORT to Name.identifier("shortArrayOf"),
            PrimitiveType.FLOAT to Name.identifier("floatArrayOf"),
            PrimitiveType.LONG to Name.identifier("longArrayOf"),
            PrimitiveType.DOUBLE to Name.identifier("doubleArrayOf")
    )

    private val ARRAY_OF_FUNCTION = Name.identifier("arrayOf")

    fun resolveCollectionLiteral(collectionLiteralExpression: KtCollectionLiteralExpression,
                                 context: ExpressionTypingContext,
                                 callResolver: CallResolver,
                                 languageVersionSettings: LanguageVersionSettings
    ): KotlinTypeInfo {
        if (!isInsideAnnotationEntryOrClass(collectionLiteralExpression)) {
            context.trace.report(UNSUPPORTED.on(collectionLiteralExpression, "Collection literals outside of annotations"))
        }

        checkSupportsArrayLiterals(collectionLiteralExpression, context, languageVersionSettings)

        return resolveCollectionLiteralSpecialMethod(collectionLiteralExpression, context, callResolver)
    }

    private fun resolveCollectionLiteralSpecialMethod(
            collectionLiteralExpression: KtCollectionLiteralExpression,
            context: ExpressionTypingContext,
            callResolver: CallResolver
    ): KotlinTypeInfo {
        val collectionLiteralCallName = getArrayFunctionCallName(context.expectedType)
        val call = CallMaker.makeCallForCollectionLiteral(collectionLiteralExpression)
        val resolutionResults = callResolver.resolveCallWithGivenName(context, call, collectionLiteralExpression, collectionLiteralCallName)

        // TODO: check that resolved function is from package `kotlin`, otherwise report an error
        if (!resolutionResults.isSingleResult) {
            return noTypeInfo(context)
        }

        context.trace.record(COLLECTION_LITERAL_CALL, collectionLiteralExpression, resolutionResults.resultingCall)
        return createTypeInfo(resolutionResults.resultingDescriptor.returnType, context)
    }

    fun checkSupportsArrayLiterals(expression: KtCollectionLiteralExpression,
                                   context: ExpressionTypingContext,
                                   languageVersionSettings: LanguageVersionSettings
    ) {
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
            return ARRAY_OF_FUNCTION
        }

        val descriptor = expectedType.constructor.declarationDescriptor ?: return ARRAY_OF_FUNCTION

        val arrayFqName = DescriptorUtils.getFqName(descriptor)
        val primitiveType = KotlinBuiltIns.getPrimitiveTypeByArrayClassFqName(arrayFqName)

        return PRIMITIVE_TYPE_TO_ARRAY[primitiveType] ?: ARRAY_OF_FUNCTION
    }
}