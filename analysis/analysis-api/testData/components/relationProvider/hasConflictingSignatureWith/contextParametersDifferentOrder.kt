interface ContextParameterTypeA
interface ContextParameterTypeB

context(a: ContextParameterTypeA, b: ContextParameterTypeB) fun conflict<caret_1>ingTopLevelFunction1() {}
context(b: ContextParameterTypeB, a: ContextParameterTypeA) fun conflict<caret_2>ingTopLevelFunction1() {}

// LANGUAGE: +ContextParameters
