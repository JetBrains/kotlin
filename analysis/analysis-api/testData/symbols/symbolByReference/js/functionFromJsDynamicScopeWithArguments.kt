// TARGET_PLATFORM: JS

fun foo(jsObject: dynamic) {
    jsObject.dynamic<caret>FunctionCall(1, "str")
}
