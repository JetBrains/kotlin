interface ContextParameterType
typealias ContextParameterTypeAlias = ContextParameterType

context(ctx: ContextParameterType) fun conflictingT<caret_1>opLevelFunction1() {}
context(ctx: ContextParameterTypeAlias) fun conflicting<caret_2>TopLevelFunction1() {}

// LANGUAGE: +ContextParameters
