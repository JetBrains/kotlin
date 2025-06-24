interface ContextParameterTypeA
interface ContextParameterTypeB

context(a: ContextParameterTypeA) fun conflicti<caret_1>ngTopLevelFunction1(): {}
context(a: ContextParameterTypeA, b: ContextParameterTypeB) fun conflictingT<caret_2>opLevelFunction1(): {}

// LANGUAGE: +ContextParameters
