interface ContextParameterType

@Deprecated("", level = DeprecationLevel.HIDDEN)
context(ctx: ContextParameterType) fun validMember<caret_1>FunctionViaHidingDeprecation1() {}

context(ctx: ContextParameterType) fun validMemberFun<caret_2>ctionViaHidingDeprecation1() {}

// LANGUAGE: +ContextParameters
