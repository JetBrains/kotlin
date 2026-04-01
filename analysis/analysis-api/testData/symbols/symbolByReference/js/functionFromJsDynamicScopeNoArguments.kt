// TARGET_PLATFORM: JS
// DO_NOT_REQUIRE_SYMBOL_RESTORATION_K1

fun foo(jsObject: dynamic) {
    jsObject.dynamic<caret>FunctionCall()
}