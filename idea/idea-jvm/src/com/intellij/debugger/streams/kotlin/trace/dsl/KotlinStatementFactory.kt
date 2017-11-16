// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.debugger.streams.kotlin.trace.dsl

import com.intellij.debugger.streams.trace.dsl.*
import com.intellij.debugger.streams.trace.dsl.impl.AssignmentStatement
import com.intellij.debugger.streams.trace.dsl.impl.TextExpression
import com.intellij.debugger.streams.trace.dsl.impl.VariableImpl
import com.intellij.debugger.streams.trace.impl.handler.type.GenericType
import com.intellij.debugger.streams.wrapper.IntermediateStreamCall
/**
 * @author Vitaliy.Bibaev
 */
class KotlinStatementFactory(private val peekCallFactory: PeekCallFactory) : StatementFactory {
  override fun createNewListExpression(elementType: GenericType, vararg args: Expression): Expression =
    TextExpression("kotlin.collections.mutableListOf<${elementType.genericTypeName}>(${StatementFactory.commaSeparate(*args)})")

  override fun createListVariable(elementType: GenericType, name: String): ListVariable = KotlinListVariable(types.list(elementType), name)

  override fun not(expression: Expression): Expression = TextExpression("!${expression.toCode()}")

  override val types: Types = KotlinTypes

  override fun createEmptyCompositeCodeBlock(): CompositeCodeBlock = KotlinCodeBlock(this)

  override fun createEmptyCodeBlock(): CodeBlock = KotlinCodeBlock(this)

  override fun createVariableDeclaration(variable: Variable, isMutable: Boolean): VariableDeclaration =
    KotlinVariableDeclaration(variable, isMutable)

  override fun createVariableDeclaration(variable: Variable, init: Expression, isMutable: Boolean): VariableDeclaration =
    KotlinVariableDeclaration(variable, isMutable, init.toCode())

  override fun createEmptyForLoopBody(iterateVariable: Variable): ForLoopBody = KotlinForLoopBody(iterateVariable, this)

  override fun createForEachLoop(iterateVariable: Variable, collection: Expression, loopBody: ForLoopBody): Convertable =
    KotlinForEachLoop(iterateVariable, collection, loopBody)

  override fun createForLoop(initialization: VariableDeclaration, condition: Expression,
                             afterThought: Expression, loopBody: ForLoopBody): Convertable =
    KotlinForLoop(initialization, condition, afterThought, loopBody)

  override fun createEmptyLambdaBody(argName: String): LambdaBody = KotlinLambdaBody(TextExpression(argName), this)

  override fun createLambda(argName: String, lambdaBody: LambdaBody): Lambda = KotlinLambda(argName, lambdaBody)

  override fun createVariable(type: GenericType, name: String): Variable = VariableImpl(type, name)

  override fun and(left: Expression, right: Expression): Expression = TextExpression("${left.toCode()} && ${right.toCode()}")

  override fun equals(left: Expression, right: Expression): Expression = TextExpression("${left.toCode()} == ${right.toCode()}")

  override fun same(left: Expression, right: Expression): Expression = TextExpression("${left.toCode()} === ${right.toCode()}")

  override fun createIfBranch(condition: Expression, thenBlock: CodeBlock): IfBranch =
    KotlinIfBranch(condition, thenBlock, this)

  override fun createAssignmentStatement(variable: Variable, expression: Expression): AssignmentStatement =
    KotlinAssignmentStatement(variable, expression)

  override fun createMapVariable(keyType: GenericType, valueType: GenericType, name: String, linked: Boolean): MapVariable =
    KotlinMapVariable(if (linked) types.linkedMap(keyType, valueType) else types.map(keyType, valueType), name)

  override fun createArrayVariable(elementType: GenericType, name: String): ArrayVariable =
    KotlinArrayVariable(types.array(elementType), name)

  override fun createScope(codeBlock: CodeBlock): Convertable =
    object : Convertable {
      override fun toCode(indent: Int): String =
      "run {\n".withIndent(indent) +
        codeBlock.toCode(indent + 1) +
        "}".withIndent(indent)
    }

  override fun createTryBlock(block: CodeBlock): TryBlock = KotlinTryBlock(block, this)

  override fun createTimeVariableDeclaration(): VariableDeclaration =
    KotlinVariableDeclaration(createVariable(types.TIME, "time"), false, types.TIME.defaultValue)

  override fun currentTimeExpression(): Expression = TextExpression("time.get()")

  override fun updateCurrentTimeExpression(): Expression = TextExpression("time.incrementAndGet()")

  override fun createNewArrayExpression(elementType: GenericType, vararg args: Expression): Expression {
    val arguments = args.joinToString { it.toCode() }
    return when (elementType) {
      types.BOOLEAN -> TextExpression("kotlin.booleanArrayOf($arguments)")
      types.INT -> TextExpression("kotlin.intArrayOf($arguments)")
      types.LONG -> TextExpression("kotlin.longArrayOf($arguments)")
      types.DOUBLE -> TextExpression("kotlin.doubleArrayOf($arguments)")
      else -> TextExpression("kotlin.arrayOf<${types.nullable { elementType }.genericTypeName}>($arguments)")
    }
  }

  override fun createNewSizedArray(elementType: GenericType, size: Expression): Expression =
      TextExpression(types.array(elementType).sizedDeclaration(size.toCode()))


  override fun createPeekCall(elementsType: GenericType, lambda: String): IntermediateStreamCall =
      peekCallFactory.createPeekCall(elementsType, lambda)
}
