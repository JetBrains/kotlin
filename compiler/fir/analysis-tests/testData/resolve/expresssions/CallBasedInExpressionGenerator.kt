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
    private val resolvedCall = operatorReference.<!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>getResolvedCallWithAssert<!>(codegen.<!UNRESOLVED_REFERENCE!>bindingContext<!>)<!>
    private val isInverted = operatorReference.<!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>getReferencedNameElementType<!>()<!> == <!UNRESOLVED_REFERENCE!>KtTokens<!>.<!UNRESOLVED_REFERENCE!>NOT_IN<!>

    override fun generate(argument: StackValue): BranchedValue =
        gen(argument).let { if (isInverted) <!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>Invert<!>(it)<!> else it }

    private fun gen(argument: StackValue): BranchedValue =
        object : <!INAPPLICABLE_CANDIDATE{LT}!><!INAPPLICABLE_CANDIDATE{PSI}!>BranchedValue<!>(argument, null, argument.<!UNRESOLVED_REFERENCE!>type<!>, <!UNRESOLVED_REFERENCE!>Opcodes<!>.<!UNRESOLVED_REFERENCE!>IFEQ<!>)<!> {
            override fun putSelector(type: Type, kotlinType: KotlinType?, v: InstructionAdapter) {
                invokeFunction(v)
                <!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>coerceTo<!>(type, kotlinType, v)<!>
            }

            override fun condJump(jumpLabel: Label, v: InstructionAdapter, jumpIfFalse: Boolean) {
                invokeFunction(v)
                v.<!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>visitJumpInsn<!>(if (jumpIfFalse) <!UNRESOLVED_REFERENCE!>Opcodes<!>.<!UNRESOLVED_REFERENCE!>IFEQ<!> else <!UNRESOLVED_REFERENCE!>Opcodes<!>.<!UNRESOLVED_REFERENCE!>IFNE<!>, jumpLabel)<!>
            }

            private fun invokeFunction(v: InstructionAdapter) {
                val result = codegen.<!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>invokeFunction<!>(resolvedCall.<!UNRESOLVED_REFERENCE!>call<!>, resolvedCall, <!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>none<!>()<!>)<!>
                result.<!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>put<!>(result.<!UNRESOLVED_REFERENCE!>type<!>, result.<!UNRESOLVED_REFERENCE!>kotlinType<!>, v)<!>
            }
        }
}
