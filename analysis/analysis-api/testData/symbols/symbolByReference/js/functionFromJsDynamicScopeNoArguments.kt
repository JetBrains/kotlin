// TARGET_PLATFORM: JS
// DO_NOT_CHECK_SYMBOL_RESTORE

fun foo(jsObject: dynamic) {
    jsObject.dynamic<caret>FunctionCall()
}