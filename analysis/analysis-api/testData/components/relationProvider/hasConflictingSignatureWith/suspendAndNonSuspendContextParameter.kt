interface ContextParameterType

context(ctx: () -> ContextParameterType) fun validTopLeve<caret_1>lFunction() {}
context(ctx: suspend () -> ContextParameterType) fun validTopL<caret_2>evelFunction() {}

// LANGUAGE: +ContextParameters
