/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.cfg.UnreachableCode
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.modalityModifier
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifier
import org.jetbrains.kotlin.resolve.multiplatform.K1ExpectActualCompatibility

object ClassicPositioningStrategies {
    @JvmField
    val ACTUAL_DECLARATION_NAME: PositioningStrategy<KtNamedDeclaration> =
        object : PositioningStrategies.DeclarationHeader<KtNamedDeclaration>() {
            override fun mark(element: KtNamedDeclaration): List<TextRange> {
                val nameIdentifier = element.nameIdentifier
                return when {
                    nameIdentifier != null -> markElement(nameIdentifier)
                    element is KtNamedFunction -> PositioningStrategies.DECLARATION_SIGNATURE.mark(element)
                    else -> PositioningStrategies.DEFAULT.mark(element)
                }
            }
        }

    // TODO: move to specific strategies
    private val DiagnosticMarker.firstIncompatibility: K1ExpectActualCompatibility.Incompatible<MemberDescriptor>?
        get() {
            @Suppress("UNCHECKED_CAST")
            val map = when (factoryName) {
                Errors.NO_ACTUAL_FOR_EXPECT.name -> (this as DiagnosticWithParameters3Marker<*, *, *>).c
                Errors.ACTUAL_WITHOUT_EXPECT.name -> (this as DiagnosticWithParameters2Marker<*, *>).b
                else -> null
            } as? Map<K1ExpectActualCompatibility.Incompatible<MemberDescriptor>, Collection<MemberDescriptor>> ?: return null
            return map.keys.firstOrNull()
        }

    @JvmField
    val INCOMPATIBLE_DECLARATION: PositioningStrategy<KtNamedDeclaration> =
        object : PositioningStrategies.DeclarationHeader<KtNamedDeclaration>() {
            override fun markDiagnostic(diagnostic: DiagnosticMarker): List<TextRange> {
                val element = diagnostic.psiElement as KtNamedDeclaration
                val callableDeclaration = element as? KtCallableDeclaration
                val incompatibility = diagnostic.firstIncompatibility
                return when (incompatibility) {
                    null, is K1ExpectActualCompatibility.Incompatible.ClassScopes,
                    K1ExpectActualCompatibility.Incompatible.EnumEntries -> null
                    K1ExpectActualCompatibility.Incompatible.ClassKind -> {
                        val startElement =
                            element.modifierList?.getModifier(KtTokens.ENUM_KEYWORD)
                                ?: element.modifierList?.getModifier(KtTokens.ANNOTATION_KEYWORD)
                        val endElement =
                            element.node.findChildByType(PositioningStrategies.classKindTokens)?.psi
                                ?: element.nameIdentifier
                        if (startElement != null && endElement != null) {
                            return markRange(startElement, endElement)
                        } else {
                            endElement
                        }
                    }
                    K1ExpectActualCompatibility.Incompatible.TypeParameterNames,
                    K1ExpectActualCompatibility.Incompatible.FunctionTypeParameterCount,
                    K1ExpectActualCompatibility.Incompatible.ClassTypeParameterCount,
                    K1ExpectActualCompatibility.Incompatible.FunctionTypeParameterUpperBounds,
                    K1ExpectActualCompatibility.Incompatible.ClassTypeParameterUpperBounds,
                    K1ExpectActualCompatibility.Incompatible.TypeParameterVariance,
                    K1ExpectActualCompatibility.Incompatible.TypeParameterReified -> {
                        (element as? KtTypeParameterListOwner)?.typeParameterList
                    }
                    K1ExpectActualCompatibility.Incompatible.CallableKind -> {
                        (callableDeclaration as? KtNamedFunction)?.funKeyword
                            ?: (callableDeclaration as? KtProperty)?.valOrVarKeyword
                    }
                    K1ExpectActualCompatibility.Incompatible.ParameterShape -> {
                        callableDeclaration?.let { it.receiverTypeReference ?: it.valueParameterList }
                    }
                    K1ExpectActualCompatibility.Incompatible.ParameterCount, K1ExpectActualCompatibility.Incompatible.ParameterTypes,
                    K1ExpectActualCompatibility.Incompatible.ParameterNames, K1ExpectActualCompatibility.Incompatible.ValueParameterVararg,
                    K1ExpectActualCompatibility.Incompatible.ActualFunctionWithDefaultParameters,
                    K1ExpectActualCompatibility.Incompatible.ValueParameterNoinline,
                    K1ExpectActualCompatibility.Incompatible.ValueParameterCrossinline -> {
                        callableDeclaration?.valueParameterList
                    }
                    K1ExpectActualCompatibility.Incompatible.ReturnType -> {
                        callableDeclaration?.typeReference
                    }
                    K1ExpectActualCompatibility.Incompatible.FunctionModifiersDifferent,
                    K1ExpectActualCompatibility.Incompatible.FunctionModifiersNotSubset,
                    K1ExpectActualCompatibility.Incompatible.PropertyLateinitModifier,
                    K1ExpectActualCompatibility.Incompatible.PropertyConstModifier,
                    K1ExpectActualCompatibility.Incompatible.ClassModifiers,
                    K1ExpectActualCompatibility.Incompatible.FunInterfaceModifier -> {
                        element.modifierList
                    }
                    K1ExpectActualCompatibility.Incompatible.PropertyKind -> {
                        element.node.findChildByType(PositioningStrategies.propertyKindTokens)?.psi
                    }
                    K1ExpectActualCompatibility.Incompatible.Supertypes -> {
                        (element as? KtClassOrObject)?.getSuperTypeList()
                    }
                    K1ExpectActualCompatibility.Incompatible.Modality -> {
                        element.modalityModifier()
                    }
                    K1ExpectActualCompatibility.Incompatible.Visibility -> {
                        element.visibilityModifier()
                    }
                    K1ExpectActualCompatibility.Incompatible.PropertySetterVisibility -> {
                        (element as? KtProperty)?.setter?.modifierList
                    }
                }?.let { markElement(it) } ?: ACTUAL_DECLARATION_NAME.mark(element)
            }
        }

    @JvmField
    val UNREACHABLE_CODE: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
        override fun markDiagnostic(diagnostic: DiagnosticMarker): List<TextRange> {
            @Suppress("UNCHECKED_CAST")
            val unreachableCode = diagnostic as DiagnosticWithParameters2Marker<Set<KtElement>, Set<KtElement>>
            return UnreachableCode.getUnreachableTextRanges(unreachableCode.psiElement as KtElement, unreachableCode.a, unreachableCode.b)
        }
    }

}
