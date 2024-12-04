/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.UnsignedTypes
import org.jetbrains.kotlin.config.LanguageFeature.*
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.incremental.KotlinLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtCollectionLiteralExpression
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.resolve.BindingContext.COLLECTION_LITERAL_CALL
import org.jetbrains.kotlin.resolve.CollectionLiteralResolver.ContainerKind.*
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.util.CallMaker
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
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
        when (computeKindOfContainer(collectionLiteralExpression)) {
            AnnotationOrAnnotationClass -> {}
            CompanionOfAnnotation -> {
                val factory = when (context.languageVersionSettings.supportsFeature(ProhibitArrayLiteralsInCompanionOfAnnotation)) {
                    true -> UNSUPPORTED
                    false -> UNSUPPORTED_WARNING
                }
                reportUnsupportedLiteral(context, factory, collectionLiteralExpression)
            }
            Other -> reportUnsupportedLiteral(context, UNSUPPORTED, collectionLiteralExpression)
        }

        return resolveCollectionLiteralSpecialMethod(collectionLiteralExpression, context)
    }

    private fun reportUnsupportedLiteral(
        context: ExpressionTypingContext,
        diagnosticFactory: DiagnosticFactory1<PsiElement, String>,
        collectionLiteralExpression: KtCollectionLiteralExpression
    ) {
        context.trace.report(diagnosticFactory.on(collectionLiteralExpression, "Collection literals outside of annotations"))
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
        val memberScopeOfKotlinPackage = module.getPackage(StandardNames.BUILT_INS_PACKAGE_FQ_NAME).memberScope
        return memberScopeOfKotlinPackage.getContributedFunctions(callName, KotlinLookupLocation(expression))
    }

    private enum class ContainerKind {
        AnnotationOrAnnotationClass,
        CompanionOfAnnotation,
        Other
    }

    private fun computeKindOfContainer(expression: KtCollectionLiteralExpression): ContainerKind {
        val parent = PsiTreeUtil.getParentOfType(expression, KtAnnotationEntry::class.java, KtClass::class.java, KtObjectDeclaration::class.java)
        if (parent is KtObjectDeclaration) {
            val containingAnnotation = PsiTreeUtil.getParentOfType(parent, KtClass::class.java)
            if (containingAnnotation != null && containingAnnotation.isAnnotation()) {
                return CompanionOfAnnotation
            }
        }
        return if (parent is KtAnnotationEntry || (parent is KtClass && parent.isAnnotation())) {
            AnnotationOrAnnotationClass
        } else {
            Other
        }
    }

    private fun getArrayFunctionCallName(expectedType: KotlinType): Name {
        if (TypeUtils.noExpectedType(expectedType) ||
            !(KotlinBuiltIns.isPrimitiveArray(expectedType) || KotlinBuiltIns.isUnsignedArrayType(expectedType))
        ) {
            return ArrayFqNames.ARRAY_OF_FUNCTION
        }

        val descriptor = expectedType.constructor.declarationDescriptor ?: return ArrayFqNames.ARRAY_OF_FUNCTION

        return ArrayFqNames.PRIMITIVE_TYPE_TO_ARRAY[KotlinBuiltIns.getPrimitiveArrayType(descriptor)]
            ?: UnsignedTypes.unsignedArrayTypeToArrayCall[UnsignedTypes.toUnsignedArrayType(descriptor)]
            ?: ArrayFqNames.ARRAY_OF_FUNCTION
    }
}
