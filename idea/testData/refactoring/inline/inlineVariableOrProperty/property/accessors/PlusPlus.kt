// ERROR: The following usages are not supported by the Inline refactoring. They won't be processed.\nUnsupported usage: property++

var <caret>property: Int
    get() = 1
    set(value) {
    }

fun foo() {
    property++
}