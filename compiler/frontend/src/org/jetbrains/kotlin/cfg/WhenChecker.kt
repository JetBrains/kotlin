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

package org.jetbrains.kotlin.cfg

import org.jetbrains.kotlin.psi.*;
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.checkReservedPrefixWord
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.CompileTimeConstantUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils

import java.util.HashSet

import org.jetbrains.kotlin.resolve.DescriptorUtils.isEnumClass
import org.jetbrains.kotlin.resolve.DescriptorUtils.isEnumEntry
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression

object WhenChecker {

    @JvmStatic
    fun mustHaveElse(expression: KtWhenExpression, trace: BindingTrace): Boolean {
        return expression.isUsedAsExpression(trace.bindingContext) && !isWhenExhaustive(expression, trace)
    }

    @JvmStatic
    fun isWhenByEnum(expression: KtWhenExpression, context: BindingContext): Boolean {
        return getClassDescriptorOfTypeIfEnum(whenSubjectType(expression, context)) != null
    }

    @JvmStatic
    fun getClassDescriptorOfTypeIfEnum(type: KotlinType?): ClassDescriptor? {
        if (type == null) return null
        val classDescriptor = TypeUtils.getClassDescriptor(type) ?: return null
        if (classDescriptor.kind != ClassKind.ENUM_CLASS) return null

        return classDescriptor
    }

    private fun whenSubjectType(expression: KtWhenExpression, context: BindingContext): KotlinType? =
            expression.subjectExpression?.let { context.getType(it) } ?: null

    private fun isWhenOnBooleanExhaustive(expression: KtWhenExpression, trace: BindingTrace): Boolean {
        // It's assumed (and not checked) that expression is of the boolean type
        var containsFalse = false
        var containsTrue = false
        for (whenEntry in expression.entries) {
            for (whenCondition in whenEntry.conditions) {
                if (whenCondition is KtWhenConditionWithExpression) {
                    val whenExpression = whenCondition.expression
                    if (CompileTimeConstantUtils.canBeReducedToBooleanConstant(whenExpression, trace, true)) containsTrue = true
                    if (CompileTimeConstantUtils.canBeReducedToBooleanConstant(whenExpression, trace, false)) containsFalse = true
                }
            }
        }
        return containsFalse && containsTrue
    }

    @JvmStatic
    fun isWhenOnEnumExhaustive(
            expression: KtWhenExpression,
            trace: BindingTrace,
            enumClassDescriptor: ClassDescriptor): Boolean {
        assert(isEnumClass(enumClassDescriptor)) { "isWhenOnEnumExhaustive should be called with an enum class descriptor" }
        val entryDescriptors =
                DescriptorUtils.getAllDescriptors(enumClassDescriptor.unsubstitutedInnerClassesScope)
                        .filter { isEnumEntry(it) }
                        .filterIsInstance<ClassDescriptor>()
                        .toSet()
        return !entryDescriptors.isEmpty() && containsAllClassCases(expression, entryDescriptors, trace)
    }

    private fun collectNestedSubclasses(
            baseDescriptor: ClassDescriptor,
            currentDescriptor: ClassDescriptor,
            subclasses: MutableSet<ClassDescriptor>) {
        for (descriptor in DescriptorUtils.getAllDescriptors(currentDescriptor.unsubstitutedInnerClassesScope)) {
            if (descriptor is ClassDescriptor) {
                if (DescriptorUtils.isDirectSubclass(descriptor, baseDescriptor)) {
                    subclasses.add(descriptor)
                }
                collectNestedSubclasses(baseDescriptor, descriptor, subclasses)
            }
        }
    }

    private fun isWhenOnSealedClassExhaustive(
            expression: KtWhenExpression,
            trace: BindingTrace,
            classDescriptor: ClassDescriptor): Boolean {
        assert(classDescriptor.modality === Modality.SEALED) { "isWhenOnSealedClassExhaustive should be called with a sealed class descriptor" }
        val memberClassDescriptors = HashSet<ClassDescriptor>()
        collectNestedSubclasses(classDescriptor, classDescriptor, memberClassDescriptors)
        // When on a sealed class without derived members is considered non-exhaustive (see test WhenOnEmptySealed)
        return !memberClassDescriptors.isEmpty() && containsAllClassCases(expression, memberClassDescriptors, trace)
    }

    /**
     * It's assumed that function is called for a final type. In this case the only possible smart cast is to not nullable type.
     * @return true if type is nullable, and cannot be smart casted
     */
    private fun isNullableTypeWithoutPossibleSmartCast(
            expression: KtExpression?,
            type: KotlinType,
            context: BindingContext): Boolean {
        if (expression == null) return false // Normally should not happen
        if (!TypeUtils.isNullableType(type)) return false
        // We cannot read data flow information here due to lack of inputs (module descriptor is necessary)
        if (context.get(BindingContext.SMARTCAST, expression) != null) {
            // We have smart cast from enum or boolean to something
            // Not very nice but we *can* decide it was smart cast to not-null
            // because both enum and boolean are final
            return false
        }
        return true
    }

    @JvmStatic
    fun isWhenExhaustive(expression: KtWhenExpression, trace: BindingTrace): Boolean {
        val type = whenSubjectType(expression, trace.bindingContext) ?: return false
        val enumClassDescriptor = getClassDescriptorOfTypeIfEnum(type)

        val exhaustive: Boolean
        if (enumClassDescriptor == null) {
            if (KotlinBuiltIns.isBoolean(TypeUtils.makeNotNullable(type))) {
                exhaustive = isWhenOnBooleanExhaustive(expression, trace)
            }
            else {
                val classDescriptor = TypeUtils.getClassDescriptor(type)
                exhaustive = (classDescriptor != null &&
                              classDescriptor.modality === Modality.SEALED &&
                              isWhenOnSealedClassExhaustive(expression, trace, classDescriptor))
            }
        }
        else {
            exhaustive = isWhenOnEnumExhaustive(expression, trace, enumClassDescriptor)
        }
        if (exhaustive) {
            // Flexible (nullable) enum types are also counted as exhaustive
            if ((enumClassDescriptor != null && type.isFlexible()) ||
                containsNullCase(expression, trace) ||
                !isNullableTypeWithoutPossibleSmartCast(expression.subjectExpression, type, trace.bindingContext)) {

                trace.record(BindingContext.EXHAUSTIVE_WHEN, expression)
                return true
            }
        }
        return false
    }

    private fun containsAllClassCases(
            whenExpression: KtWhenExpression,
            memberDescriptors: Set<ClassDescriptor>,
            trace: BindingTrace): Boolean {
        val checkedDescriptors = HashSet<ClassDescriptor>()
        for (whenEntry in whenExpression.entries) {
            for (condition in whenEntry.conditions) {
                var negated = false
                var checkedDescriptor: ClassDescriptor? = null
                if (condition is KtWhenConditionIsPattern) {
                    val checkedType = trace.get(BindingContext.TYPE, condition.typeReference)
                    if (checkedType != null) {
                        checkedDescriptor = TypeUtils.getClassDescriptor(checkedType)
                    }
                    negated = condition.isNegated
                }
                else if (condition is KtWhenConditionWithExpression) {
                    if (condition.expression != null) {
                        val reference = getReference(condition.expression)
                        if (reference != null) {
                            val target = trace.get(BindingContext.REFERENCE_TARGET, reference)
                            if (target is ClassDescriptor) {
                                checkedDescriptor = target
                            }
                        }
                    }
                }

                // Checks are important only for nested subclasses of the sealed class
                // In additional, check without "is" is important only for objects
                if (checkedDescriptor == null ||
                    !memberDescriptors.contains(checkedDescriptor) ||
                    (condition is KtWhenConditionWithExpression &&
                     !DescriptorUtils.isObject(checkedDescriptor) &&
                     !DescriptorUtils.isEnumEntry(checkedDescriptor))) {
                    continue
                }
                if (negated) {
                    if (checkedDescriptors.contains(checkedDescriptor)) return true // all members are already there
                    checkedDescriptors.addAll(memberDescriptors)
                    checkedDescriptors.remove(checkedDescriptor)
                }
                else {
                    checkedDescriptors.add(checkedDescriptor)
                }
            }
        }
        return checkedDescriptors.containsAll(memberDescriptors)
    }

    @JvmStatic
    fun containsNullCase(expression: KtWhenExpression, trace: BindingTrace): Boolean {
        for (entry in expression.entries) {
            for (condition in entry.conditions) {
                if (condition is KtWhenConditionWithExpression) {
                    condition.expression?.let {
                        val type = trace.bindingContext.getType(it)
                        if (type != null && KotlinBuiltIns.isNothingOrNullableNothing(type)) {
                            return true
                        }
                    }
                }
            }
        }
        return false
    }

    private fun getReference(expression: KtExpression?): KtSimpleNameExpression? {
        if (expression is KtSimpleNameExpression) {
            return expression
        }
        if (expression is KtQualifiedExpression) {
            return getReference(expression.selectorExpression)
        }
        return null
    }

    @JvmStatic
    fun checkDeprecatedWhenSyntax(trace: BindingTrace, expression: KtWhenExpression) {
        if (expression.subjectExpression != null) return

        for (entry in expression.entries) {
            if (entry.isElse) continue
            var child: PsiElement? = entry.firstChild
            while (child != null) {
                if (child.node.elementType === KtTokens.COMMA) {
                    trace.report(Errors.COMMA_IN_WHEN_CONDITION_WITHOUT_ARGUMENT.on(child))
                }
                if (child.node.elementType === KtTokens.ARROW) break
                child = child.nextSibling
            }
        }
    }

    @JvmStatic
    fun checkReservedPrefix(trace: BindingTrace, expression: KtWhenExpression) {
        checkReservedPrefixWord(trace, expression.whenKeyword, "sealed", TokenSet.EMPTY, "sealed when")
    }
}
