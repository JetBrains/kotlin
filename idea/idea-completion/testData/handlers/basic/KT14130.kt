// CODE_STYLE_SETTING: ALIGN_MULTILINE_PARAMETERS_IN_CALLS = true
package test

interface Foo {
    companion object {
        val EMPTY = object : Foo {}
    }
}

fun test(foo: Foo, bar: String) {}

fun test2() {
    test(<caret>, "")
}

// ELEMENT: EMPTY