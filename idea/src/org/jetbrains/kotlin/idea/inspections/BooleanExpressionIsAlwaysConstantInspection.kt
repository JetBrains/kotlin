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

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.loopToCallChain.isFalseConstant
import org.jetbrains.kotlin.idea.intentions.loopToCallChain.isTrueConstant
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.KtPsiUtil.deparenthesize
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.typeUtil.isBoolean
import org.jetbrains.kotlin.types.typeUtil.isNothing

sealed class SymbolValue

data class ExpressionSymbolValue(val expression: KtExpression): SymbolValue()

data class ConstantSymbolValue(val constValue: Boolean): SymbolValue()

data class NotSymbolValue(val operand: SymbolValue): SymbolValue() {
    companion object {
        fun create(value: SymbolValue) = if (value is NotSymbolValue) value.operand else NotSymbolValue(value)
    }
}

class UnknownSymbolValue: SymbolValue()

data class BooleanValueInfo (val reference2Value: MutableMap<VariableDescriptor, SymbolValue> = LinkedHashMap(),
                            val assumes: MutableSet<SymbolValue> = LinkedHashSet()) {

    fun mergeAndUpdate(one: BooleanValueInfo, another: BooleanValueInfo) {
        val oneReference2Value = one.reference2Value
        val anotherReference2Value = another.reference2Value
        reference2Value.clear()
        assumes.clear()
        for (reference in oneReference2Value.keys.intersect(anotherReference2Value.keys)) {
            reference2Value[reference] = if (oneReference2Value[reference] == anotherReference2Value[reference])
                oneReference2Value.get(reference)!! else
                UnknownSymbolValue()
        }
        assumes.addAll(one.assumes.intersect(another.assumes))
    }

    fun replaceBy(from: BooleanValueInfo) {
        assumes.clear()
        reference2Value.clear()
        assumes.addAll(from.assumes)
        reference2Value.putAll(from.reference2Value)
    }

    fun clone() : BooleanValueInfo {
        return BooleanValueInfo(LinkedHashMap(reference2Value), LinkedHashSet(assumes))
    }

    fun assign(variableDescriptor: VariableDescriptor, value: SymbolValue) {
        reference2Value[variableDescriptor] = value
    }

    fun assume(value: SymbolValue): BooleanValueInfo {
        val newAssumes = LinkedHashSet<SymbolValue>(assumes)
        newAssumes.add(value)
        return BooleanValueInfo(LinkedHashMap(reference2Value), newAssumes)
    }

    fun assumeAll(values: List<SymbolValue>): BooleanValueInfo {
        val newAssumes = LinkedHashSet<SymbolValue>(assumes)
        newAssumes.addAll(values)
        return BooleanValueInfo(LinkedHashMap(reference2Value), newAssumes)
    }

    fun getSymbolValueForExpression(expression: KtExpression) : SymbolValue {
        when  {
            expression.isTrueConstant() -> return ConstantSymbolValue(true)
            expression.isFalseConstant() -> return ConstantSymbolValue(false)
            expression is KtUnaryExpression && expression.operationToken == KtTokens.EXCL -> {
                return NotSymbolValue.create(getSymbolValueForExpression(expression.baseExpression ?: return UnknownSymbolValue()))
            }
            expression is KtReferenceExpression -> {
                val context = expression.analyze()
                val varDecl = context[BindingContext.REFERENCE_TARGET, expression]
                if (varDecl is VariableDescriptor && (varDecl is ValueParameterDescriptor || varDecl is LocalVariableDescriptor)) {
                    if (varDecl != null && !varDecl.isVar) {
                        if (reference2Value.containsKey(varDecl)) {
                            return reference2Value[varDecl]!!
                        }
                        else {
                            val unknownValue = UnknownSymbolValue()
                            reference2Value.put(varDecl, unknownValue)
                            return unknownValue
                        }
                    }
                }
            }
        }
        return ExpressionSymbolValue(expression)
    }
}

class BooleanExpressionIsAlwaysConstantInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtTreeVisitor<BooleanValueInfo>() {

            override fun visitNamedFunction(function: KtNamedFunction, data: BooleanValueInfo?): Void? {
                return super.visitNamedFunction(function, BooleanValueInfo())
            }

            override fun visitIfExpression(ifExpression: KtIfExpression, bvInfo: BooleanValueInfo?): Void? {
                if (bvInfo == null)
                    return null

                val condition = deparenthesize(ifExpression.condition)
                val thenExpression = ifExpression.then
                val elseExpression = ifExpression.`else`

                if (condition != null && thenExpression != null) {
                    val ctx = thenExpression.analyze()
                    val thenType = thenExpression.getType(ctx)

                    condition.accept(this, bvInfo)

                    val values = ArrayList<SymbolValue>()
                    val notValues = ArrayList<SymbolValue>()

                    parseCondition(condition, bvInfo, values,  notValues)

                    val thenBooleanValueInfo = bvInfo.assumeAll(values)
                    val elseBooleanValueInfo = bvInfo.assumeAll(notValues)
                    thenExpression.accept(this, thenBooleanValueInfo)
                    val elseType = if (elseExpression != null) {
                        elseExpression.accept(this, elseBooleanValueInfo)
                        elseExpression.analyze().getType(elseExpression)
                    } else null

                    when {
                        thenType?.isNothing() == true && elseType?.isNothing() == true -> return null
                        thenType?.isNothing() == true -> bvInfo.replaceBy(elseBooleanValueInfo)
                        elseType?.isNothing() == true -> bvInfo.replaceBy(thenBooleanValueInfo)
                        else -> bvInfo.mergeAndUpdate(thenBooleanValueInfo, elseBooleanValueInfo)
                    }
                }
                return null
            }

            override fun visitBinaryExpression(expression: KtBinaryExpression, bvInfo: BooleanValueInfo?): Void? {

                if (bvInfo == null)
                    return null

                val left =  expression.left
                val right = expression.right

                if (left != null && right != null) {
                    if (expression.operationToken == KtTokens.ANDAND) {
                        left.accept(this, bvInfo)
                        val rightBVInfo = bvInfo.assume(bvInfo.getSymbolValueForExpression(left))
                        right.accept(this, rightBVInfo)
                    }
                    if (expression.operationToken == KtTokens.OROR) {
                        left.accept(this, bvInfo)
                        val rightBVInfo = bvInfo.assume(NotSymbolValue.create(bvInfo.getSymbolValueForExpression(left)))
                        right.accept(this, rightBVInfo)
                    }
                }

                return null
            }

            override fun visitLambdaExpression(expression: KtLambdaExpression, bvInfo: BooleanValueInfo?): Void? {
                if (bvInfo == null)
                    return null
                return super.visitLambdaExpression(expression, BooleanValueInfo())
            }

            override fun visitExpression(expression: KtExpression, bvInfo: BooleanValueInfo?): Void? {
                if (bvInfo == null)
                    return null
                var reported = false
                val type = expression.analyze(BodyResolveMode.FULL).getType(expression)
                if (type?.isBoolean() == true) {
                    val expressionValue = bvInfo.getSymbolValueForExpression(expression)
                    val negValue = NotSymbolValue.create(expressionValue)
                    if (bvInfo.assumes.contains(expressionValue)) {
                        holder.registerProblem(expression, "the boolean expression is always true", ProblemHighlightType.WEAK_WARNING)
                        reported = true
                    }
                    if (bvInfo.assumes.contains(negValue)) {
                        holder.registerProblem(expression, "the boolean expression is always false", ProblemHighlightType.WEAK_WARNING)
                        reported = true
                    }
                }
                if (!reported)
                    super.visitExpression(expression, bvInfo)
                return null
            }

            override fun visitNamedDeclaration(declaration: KtNamedDeclaration, bvInfo: BooleanValueInfo?): Void? {
                if (bvInfo == null)
                    return null
                super.visitNamedDeclaration(declaration, bvInfo)
                if (declaration is KtProperty && declaration.isLocal && !declaration.isVar && declaration.hasInitializer()) {
                    val context = declaration.analyze(BodyResolveMode.PARTIAL)
                    val res = context[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration]
                    if (res is LocalVariableDescriptor) {
                        bvInfo.assign(res, bvInfo.getSymbolValueForExpression(declaration.initializer!!))
                    }
                }
                return null
            }

            override fun visitWhenExpression(expression: KtWhenExpression, bvInfo: BooleanValueInfo?): Void? {
                if (bvInfo == null)
                    return null
                expression.subjectExpression?.accept(this, bvInfo)
                val entryBvInfos = ArrayList<BooleanValueInfo>()
                for (entry in expression.entries) {
                    val newInfo = bvInfo.clone()
                    entryBvInfos.add(newInfo)
                    entry.accept(this, newInfo)
                }
                val elseInfo = bvInfo.clone()
                expression.elseExpression?.accept(this, elseInfo)
                // all bvInfos should be merged here, however, for current implementation, we have no reason to merge them.
                return null
            }
        }
    }

    private fun parseCondition(condition: KtExpression, bvInfo: BooleanValueInfo, values: ArrayList<SymbolValue>, notValues: ArrayList<SymbolValue>) {
        // Try to parse top level conjunction.
        if (condition is KtBinaryExpression && condition.operationToken == KtTokens.ANDAND) {
            var curCondition = condition;
            while (curCondition is KtBinaryExpression && curCondition.operationToken == KtTokens.ANDAND) {
                val left = curCondition.left
                val right = curCondition.right
                if (right != null)
                    values.add(bvInfo.getSymbolValueForExpression(right))
                if (left != null)
                    curCondition = left
            }
            if (curCondition != null)
                values.add(bvInfo.getSymbolValueForExpression(curCondition))
        }
        else {
            val valueSymbol = bvInfo.getSymbolValueForExpression(condition)
            values.add(valueSymbol)
            notValues.add(NotSymbolValue.create(valueSymbol))
        }
    }
}