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

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget.*
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtModifierList
import org.jetbrains.kotlin.psi.KtModifierListOwner
import java.util.*

object ModifierCheckerCore {
    private enum class Compatibility {
        // modifier pair is compatible: ok (default)
        COMPATIBLE,
        // second is redundant to first: warning
        REDUNDANT,
        // first is redundant to second: warning
        REVERSE_REDUNDANT,
        // error
        REPEATED,
        // pair is deprecated, will become incompatible: warning
        DEPRECATED,
        // pair is incompatible: error
        INCOMPATIBLE,
        // same but only for functions / properties: error
        COMPATIBLE_FOR_CLASSES_ONLY
    }

    private val defaultVisibilityTargets = EnumSet.of(CLASS_ONLY, OBJECT, INTERFACE, INNER_CLASS, ENUM_CLASS, ANNOTATION_CLASS,
                                                      MEMBER_FUNCTION, TOP_LEVEL_FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER,
                                                      MEMBER_PROPERTY, TOP_LEVEL_PROPERTY, CONSTRUCTOR)

    val possibleTargetMap = mapOf<KtModifierKeywordToken, Set<KotlinTarget>>(
            ENUM_KEYWORD      to EnumSet.of(ENUM_CLASS),
            ABSTRACT_KEYWORD  to EnumSet.of(CLASS_ONLY, LOCAL_CLASS, INNER_CLASS, INTERFACE, MEMBER_PROPERTY, MEMBER_FUNCTION),
            OPEN_KEYWORD      to EnumSet.of(CLASS_ONLY, LOCAL_CLASS, INNER_CLASS, INTERFACE, MEMBER_PROPERTY, MEMBER_FUNCTION),
            FINAL_KEYWORD     to EnumSet.of(CLASS_ONLY, LOCAL_CLASS, INNER_CLASS, ENUM_CLASS, OBJECT, MEMBER_PROPERTY, MEMBER_FUNCTION),
            SEALED_KEYWORD    to EnumSet.of(CLASS_ONLY, INNER_CLASS),
            // We should have also CLASS_ONLY here because INNER_CLASS is not always perfectly identified
            INNER_KEYWORD     to EnumSet.of(CLASS_ONLY, INNER_CLASS),
            OVERRIDE_KEYWORD  to EnumSet.of(MEMBER_PROPERTY, MEMBER_FUNCTION),
            PRIVATE_KEYWORD   to defaultVisibilityTargets,
            PUBLIC_KEYWORD    to defaultVisibilityTargets,
            INTERNAL_KEYWORD  to defaultVisibilityTargets,
            PROTECTED_KEYWORD to EnumSet.of(CLASS_ONLY, OBJECT, INTERFACE, INNER_CLASS, ENUM_CLASS, ANNOTATION_CLASS,
                                            MEMBER_FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, MEMBER_PROPERTY, CONSTRUCTOR),
            IN_KEYWORD        to EnumSet.of(TYPE_PARAMETER, TYPE_PROJECTION),
            OUT_KEYWORD       to EnumSet.of(TYPE_PARAMETER, TYPE_PROJECTION),
            REIFIED_KEYWORD   to EnumSet.of(TYPE_PARAMETER),
            VARARG_KEYWORD    to EnumSet.of(VALUE_PARAMETER, PROPERTY_PARAMETER),
            COMPANION_KEYWORD to EnumSet.of(OBJECT),
            LATEINIT_KEYWORD to EnumSet.of(MEMBER_PROPERTY),
            DATA_KEYWORD      to EnumSet.of(CLASS_ONLY, INNER_CLASS, LOCAL_CLASS),
            INLINE_KEYWORD    to EnumSet.of(FUNCTION),
            NOINLINE_KEYWORD  to EnumSet.of(VALUE_PARAMETER),
            COROUTINE_KEYWORD to EnumSet.of(VALUE_PARAMETER),
            TAILREC_KEYWORD   to EnumSet.of(FUNCTION),
            SUSPEND_KEYWORD   to EnumSet.of(FUNCTION),
            EXTERNAL_KEYWORD  to EnumSet.of(FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER),
            ANNOTATION_KEYWORD to EnumSet.of(ANNOTATION_CLASS),
            CROSSINLINE_KEYWORD to EnumSet.of(VALUE_PARAMETER),
            CONST_KEYWORD     to EnumSet.of(MEMBER_PROPERTY, TOP_LEVEL_PROPERTY),
            OPERATOR_KEYWORD  to EnumSet.of(FUNCTION),
            INFIX_KEYWORD     to EnumSet.of(FUNCTION)
    )

    // NOTE: deprecated targets must be possible!
    private val deprecatedTargetMap = mapOf<KtModifierKeywordToken, Set<KotlinTarget>>()

    // NOTE: redundant targets must be possible!
    private val redundantTargetMap = mapOf<KtModifierKeywordToken, Set<KotlinTarget>>(
            OPEN_KEYWORD  to EnumSet.of(INTERFACE)
    )

    val possibleParentTargetMap = mapOf<KtModifierKeywordToken, Set<KotlinTarget>>(
            INNER_KEYWORD     to EnumSet.of(CLASS_ONLY, INNER_CLASS, LOCAL_CLASS, ENUM_CLASS),
            OVERRIDE_KEYWORD  to EnumSet.of(CLASS_ONLY, INNER_CLASS, LOCAL_CLASS, OBJECT, OBJECT_LITERAL,
                                            INTERFACE, ENUM_CLASS, ENUM_ENTRY),
            PROTECTED_KEYWORD to EnumSet.of(CLASS_ONLY, INNER_CLASS, LOCAL_CLASS, ENUM_CLASS, COMPANION_OBJECT),
            INTERNAL_KEYWORD  to EnumSet.of(CLASS_ONLY, INNER_CLASS, LOCAL_CLASS, OBJECT, OBJECT_LITERAL,
                                            ENUM_CLASS, ENUM_ENTRY, FILE),
            PRIVATE_KEYWORD   to EnumSet.of(CLASS_ONLY, INNER_CLASS, LOCAL_CLASS, OBJECT, OBJECT_LITERAL,
                                            INTERFACE, ENUM_CLASS, ENUM_ENTRY, FILE),
            COMPANION_KEYWORD to EnumSet.of(CLASS_ONLY, ENUM_CLASS, INTERFACE),
            FINAL_KEYWORD     to EnumSet.of(CLASS_ONLY, INNER_CLASS, LOCAL_CLASS, OBJECT, OBJECT_LITERAL,
                                            ENUM_CLASS, ENUM_ENTRY, ANNOTATION_CLASS, FILE)
    )

    val deprecatedParentTargetMap = mapOf<KtModifierKeywordToken, Set<KotlinTarget>>()

    // First modifier in pair should be also first in declaration
    private val mutualCompatibility = buildCompatibilityMap()

    private fun buildCompatibilityMap(): Map<Pair<KtModifierKeywordToken, KtModifierKeywordToken>, Compatibility> {
        val result = hashMapOf<Pair<KtModifierKeywordToken, KtModifierKeywordToken>, Compatibility>()
        // Variance: in + out are incompatible
        result += incompatibilityRegister(IN_KEYWORD, OUT_KEYWORD)
        // Visibilities: incompatible
        result += incompatibilityRegister(PRIVATE_KEYWORD, PROTECTED_KEYWORD, PUBLIC_KEYWORD, INTERNAL_KEYWORD)
        // Abstract + open + final + sealed: incompatible
        result += incompatibilityRegister(ABSTRACT_KEYWORD, OPEN_KEYWORD, FINAL_KEYWORD, SEALED_KEYWORD)
        // data + open, data + inner, data + abstract, data + sealed
        result += incompatibilityRegister(DATA_KEYWORD, OPEN_KEYWORD)
        result += incompatibilityRegister(DATA_KEYWORD, INNER_KEYWORD)
        result += incompatibilityRegister(DATA_KEYWORD, ABSTRACT_KEYWORD)
        result += incompatibilityRegister(DATA_KEYWORD, SEALED_KEYWORD)
        // open is redundant to abstract & override
        result += redundantRegister(ABSTRACT_KEYWORD, OPEN_KEYWORD)
        // abstract is redundant to sealed
        result += redundantRegister(SEALED_KEYWORD, ABSTRACT_KEYWORD)

        // const is incompatible with abstract, open, override
        result += incompatibilityRegister(CONST_KEYWORD, ABSTRACT_KEYWORD)
        result += incompatibilityRegister(CONST_KEYWORD, OPEN_KEYWORD)
        result += incompatibilityRegister(CONST_KEYWORD, OVERRIDE_KEYWORD)

        // private is incompatible with override
        result += incompatibilityRegister(PRIVATE_KEYWORD, OVERRIDE_KEYWORD)
        // private is compatible with open / abstract only for classes
        result += compatibilityForClassesRegister(PRIVATE_KEYWORD, OPEN_KEYWORD)
        result += compatibilityForClassesRegister(PRIVATE_KEYWORD, ABSTRACT_KEYWORD)

        result += incompatibilityRegister(CROSSINLINE_KEYWORD, NOINLINE_KEYWORD)
        return result
    }

    private fun redundantRegister(
            sufficient: KtModifierKeywordToken,
            redundant: KtModifierKeywordToken
    ): Map<Pair<KtModifierKeywordToken, KtModifierKeywordToken>, Compatibility> {
        return mapOf(Pair(sufficient, redundant) to Compatibility.REDUNDANT,
                     Pair(redundant, sufficient) to Compatibility.REVERSE_REDUNDANT)
    }

    private fun compatibilityRegister(
            compatibility: Compatibility, vararg list: KtModifierKeywordToken
    ): Map<Pair<KtModifierKeywordToken, KtModifierKeywordToken>, Compatibility> {
        val result = hashMapOf<Pair<KtModifierKeywordToken, KtModifierKeywordToken>, Compatibility>()
        for (first in list) {
            for (second in list) {
                if (first != second) {
                    result[Pair(first, second)] = compatibility
                }
            }
        }
        return result
    }

    private fun compatibilityForClassesRegister(vararg list: KtModifierKeywordToken) =
            compatibilityRegister(Compatibility.COMPATIBLE_FOR_CLASSES_ONLY, *list)

    private fun incompatibilityRegister(vararg list: KtModifierKeywordToken) = compatibilityRegister(Compatibility.INCOMPATIBLE, *list)

    private fun deprecationRegister(vararg list: KtModifierKeywordToken) = compatibilityRegister(Compatibility.DEPRECATED, *list)

    private fun compatibility(first: KtModifierKeywordToken, second: KtModifierKeywordToken): Compatibility {
        if (first == second) {
            return Compatibility.REPEATED
        }
        else {
            return mutualCompatibility[Pair(first, second)] ?: Compatibility.COMPATIBLE
        }
    }

    private fun checkCompatibility(trace: BindingTrace,
                                   firstNode: ASTNode,
                                   secondNode: ASTNode,
                                   owner: PsiElement,
                                   incorrectNodes: MutableSet<ASTNode>) {
        val first = firstNode.elementType as KtModifierKeywordToken
        val second = secondNode.elementType as KtModifierKeywordToken
        val compatibility = compatibility(first, second)
        when (compatibility) {
            Compatibility.COMPATIBLE -> {}
            Compatibility.REPEATED -> if (incorrectNodes.add(secondNode)) {
                trace.report(Errors.REPEATED_MODIFIER.on (secondNode.psi, first))
            }
            Compatibility.REDUNDANT ->
                trace.report(Errors.REDUNDANT_MODIFIER.on(secondNode.psi, second, first))
            Compatibility.REVERSE_REDUNDANT ->
                trace.report(Errors.REDUNDANT_MODIFIER.on(firstNode.psi,  first, second))
            Compatibility.DEPRECATED -> {
                trace.report(Errors.DEPRECATED_MODIFIER_PAIR.on(firstNode.psi, first, second))
                trace.report(Errors.DEPRECATED_MODIFIER_PAIR.on(secondNode.psi, second, first))
            }
            Compatibility.COMPATIBLE_FOR_CLASSES_ONLY, Compatibility.INCOMPATIBLE -> {
                if (compatibility == Compatibility.COMPATIBLE_FOR_CLASSES_ONLY) {
                    if (owner is KtClassOrObject) return
                }
                if (incorrectNodes.add(firstNode)) {
                    trace.report(Errors.INCOMPATIBLE_MODIFIERS.on(firstNode.psi, first, second))
                }
                if (incorrectNodes.add(secondNode)) {
                    trace.report(Errors.INCOMPATIBLE_MODIFIERS.on(secondNode.psi, second, first))
                }
            }
        }
    }

    // Should return false if error is reported, true otherwise
    private fun checkTarget(trace: BindingTrace, node: ASTNode, actualTargets: List<KotlinTarget>): Boolean {
        val modifier = node.elementType as KtModifierKeywordToken
        val possibleTargets = possibleTargetMap[modifier] ?: emptySet()
        if (!actualTargets.any { it in possibleTargets }) {
            trace.report(Errors.WRONG_MODIFIER_TARGET.on(node.psi, modifier, actualTargets.firstOrNull()?.description ?: "this"))
            return false
        }
        val deprecatedTargets = deprecatedTargetMap[modifier] ?: emptySet()
        val redundantTargets = redundantTargetMap[modifier] ?: emptySet()
        if (actualTargets.any { it in deprecatedTargets }) {
            trace.report(Errors.DEPRECATED_MODIFIER_FOR_TARGET.on(node.psi, modifier, actualTargets.firstOrNull()?.description ?: "this"))
        }
        else if (actualTargets.any { it in redundantTargets }) {
            trace.report(Errors.REDUNDANT_MODIFIER_FOR_TARGET.on(node.psi, modifier, actualTargets.firstOrNull()?.description ?: "this"))
        }
        return true
    }

    // Should return false if error is reported, true otherwise
    private fun checkParent(trace: BindingTrace, node: ASTNode, parentDescriptor: DeclarationDescriptor?): Boolean {
        val modifier = node.elementType as KtModifierKeywordToken
        val actualParents: List<KotlinTarget> = when (parentDescriptor) {
            is ClassDescriptor -> KotlinTarget.classActualTargets(parentDescriptor)
            is FunctionDescriptor -> listOf(FUNCTION)
            else -> listOf(FILE)
        }
        val deprecatedParents = deprecatedParentTargetMap[modifier]
        if (deprecatedParents != null && actualParents.any { it in deprecatedParents }) {
            trace.report(Errors.DEPRECATED_MODIFIER_CONTAINING_DECLARATION.on(node.psi, modifier, actualParents.firstOrNull()?.description ?: "this scope"))
            return true
        }
        val possibleParents = possibleParentTargetMap[modifier] ?: return true
        if (possibleParents == KotlinTarget.ALL_TARGET_SET) return true
        if (actualParents.any { it in possibleParents }) return true
        trace.report(Errors.WRONG_MODIFIER_CONTAINING_DECLARATION.on(node.psi, modifier, actualParents.firstOrNull()?.description ?: "this scope"))
        return false
    }

    private val MODIFIER_KEYWORD_SET = TokenSet.orSet(KtTokens.SOFT_KEYWORDS, TokenSet.create(KtTokens.IN_KEYWORD))

    private fun checkModifierList(list: KtModifierList, trace: BindingTrace, parentDescriptor: DeclarationDescriptor?, actualTargets: List<KotlinTarget>) {
        // It's a list of all nodes with error already reported
        // General strategy: report no more than one error but any number of warnings
        val incorrectNodes = hashSetOf<ASTNode>()
        val children = list.node.getChildren(MODIFIER_KEYWORD_SET)
        for (second in children) {
            for (first in children) {
                if (first == second) {
                    break
                }
                checkCompatibility(trace, first, second, list.owner, incorrectNodes)
            }
            if (second !in incorrectNodes) {
                if (!checkTarget(trace, second, actualTargets)) {
                    incorrectNodes += second
                }
                else if (!checkParent(trace, second, parentDescriptor)) {
                    incorrectNodes += second
                }
            }
        }
    }

    fun check(listOwner: KtModifierListOwner, trace: BindingTrace, descriptor: DeclarationDescriptor?) {
        if (listOwner is KtDeclarationWithBody) {
            // JetFunction or JetPropertyAccessor
            for (parameter in listOwner.valueParameters) {
                if (!parameter.hasValOrVar()) {
                    check(parameter, trace, null)
                }
            }
        }
        val actualTargets = AnnotationChecker.getDeclarationSiteActualTargetList(listOwner, descriptor as? ClassDescriptor, trace)
        val list = listOwner.modifierList ?: return
        checkModifierList(list, trace, descriptor?.containingDeclaration, actualTargets)
    }
}
