package org.jetbrains.kotlin.codegen.range.inExpression

class CallBasedInExpressionGenerator(
    val codegen: ExpressionCodegen,
    operatorReference: KtSimpleNameExpression
) : InExpressionGenerator {
    private val resolvedCall = operatorReference.<!UNRESOLVED_REFERENCE!>getResolvedCallWithAssert<!>(codegen.<!UNRESOLVED_REFERENCE!>bindingContext<!>)
    private val isInverted = operatorReference.<!UNRESOLVED_REFERENCE!>getReferencedNameElementType<!>() == <!UNRESOLVED_REFERENCE!>KtTokens<!>.<!UNRESOLVED_REFERENCE!>NOT_IN<!>

    override fun generate(argument: StackValue): BranchedValue =
        gen(argument).<!INAPPLICABLE_CANDIDATE!>let<!> { if (isInverted) <!UNRESOLVED_REFERENCE!>Invert<!>(<!UNRESOLVED_REFERENCE!>it<!>) else <!UNRESOLVED_REFERENCE!>it<!> }

    private fun gen(argument: StackValue): BranchedValue =
        object : BranchedValue(argument, null, argument.<!UNRESOLVED_REFERENCE!>type<!>, <!UNRESOLVED_REFERENCE!>Opcodes<!>.<!UNRESOLVED_REFERENCE!>IFEQ<!>) {
            override fun putSelector(type: Type, kotlinType: KotlinType?, v: InstructionAdapter) {
                invokeFunction(v)
                <!UNRESOLVED_REFERENCE!>coerceTo<!>(type, kotlinType, v)
            }

            override fun condJump(jumpLabel: Label, v: InstructionAdapter, jumpIfFalse: Boolean) {
                invokeFunction(v)
                v.<!UNRESOLVED_REFERENCE!>visitJumpInsn<!>(if (jumpIfFalse) <!UNRESOLVED_REFERENCE!>Opcodes<!>.<!UNRESOLVED_REFERENCE!>IFEQ<!> else <!UNRESOLVED_REFERENCE!>Opcodes<!>.<!UNRESOLVED_REFERENCE!>IFNE<!>, jumpLabel)
            }

            private fun invokeFunction(v: InstructionAdapter) {
                val result = codegen.<!UNRESOLVED_REFERENCE!>invokeFunction<!>(resolvedCall.<!UNRESOLVED_REFERENCE!>call<!>, resolvedCall, <!UNRESOLVED_REFERENCE!>none<!>())
                result.<!UNRESOLVED_REFERENCE!>put<!>(result.<!UNRESOLVED_REFERENCE!>type<!>, result.<!UNRESOLVED_REFERENCE!>kotlinType<!>, v)
            }
        }
}