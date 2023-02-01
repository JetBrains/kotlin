package org.jetbrains.kotlin.codegen.range.inExpression

interface ExpressionCodegen
interface KtSimpleNameExpression
interface InExpressionGenerator
interface StackValue
open class BranchedValue
interface Type
interface KotlinType
interface Label
interface InstructionAdapter

class CallBasedInExpressionGenerator(
    val codegen: ExpressionCodegen,
    operatorReference: KtSimpleNameExpression
) : InExpressionGenerator {
    private val resolvedCall = operatorReference.<!UNRESOLVED_REFERENCE!>getResolvedCallWithAssert<!>(codegen.<!UNRESOLVED_REFERENCE!>bindingContext<!>)
    private val isInverted = operatorReference.<!UNRESOLVED_REFERENCE!>getReferencedNameElementType<!>() == <!UNRESOLVED_REFERENCE!>KtTokens<!>.NOT_IN

    <!NOTHING_TO_OVERRIDE!>override<!> fun generate(argument: StackValue): BranchedValue =
        gen(argument).let { if (isInverted) <!UNRESOLVED_REFERENCE!>Invert<!>(it) else it }

    private fun gen(argument: StackValue): BranchedValue =
        object : BranchedValue(<!TOO_MANY_ARGUMENTS!>argument<!>, <!TOO_MANY_ARGUMENTS!>null<!>, <!TOO_MANY_ARGUMENTS!>argument.<!UNRESOLVED_REFERENCE!>type<!><!>, <!TOO_MANY_ARGUMENTS!><!UNRESOLVED_REFERENCE!>Opcodes<!>.IFEQ<!>) {
            <!NOTHING_TO_OVERRIDE!>override<!> fun putSelector(type: Type, kotlinType: KotlinType?, v: InstructionAdapter) {
                invokeFunction(v)
                <!UNRESOLVED_REFERENCE!>coerceTo<!>(type, kotlinType, v)
            }

            <!NOTHING_TO_OVERRIDE!>override<!> fun condJump(jumpLabel: Label, v: InstructionAdapter, jumpIfFalse: Boolean) {
                invokeFunction(v)
                v.<!UNRESOLVED_REFERENCE!>visitJumpInsn<!>(if (jumpIfFalse) <!UNRESOLVED_REFERENCE!>Opcodes<!>.IFEQ else <!UNRESOLVED_REFERENCE!>Opcodes<!>.IFNE, jumpLabel)
            }

            private fun invokeFunction(v: InstructionAdapter) {
                val result = codegen.<!UNRESOLVED_REFERENCE!>invokeFunction<!>(resolvedCall.call, resolvedCall, <!UNRESOLVED_REFERENCE!>none<!>())
                result.put(result.type, result.kotlinType, v)
            }
        }
}
