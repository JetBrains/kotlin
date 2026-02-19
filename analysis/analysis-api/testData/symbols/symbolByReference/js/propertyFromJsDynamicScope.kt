// TARGET_PLATFORM: JS
// DO_NOT_CHECK_SYMBOL_RESTORE_K1

fun foo(jsObject: dynamic) {
    jsObject.dynamic<caret>PropertyAccess
}