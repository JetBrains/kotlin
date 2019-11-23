// RUNTIME_WITH_SCRIPT_RUNTIME
// OUT_OF_CODE_BLOCK: TRUE
// ERROR: Too many arguments for public final fun foo(): Int defined in ScriptTopLevelCallExpression
// ERROR: Unresolved reference: a
fun foo() = 1

foo(<caret>)

// SKIP_ANALYZE_CHECK
