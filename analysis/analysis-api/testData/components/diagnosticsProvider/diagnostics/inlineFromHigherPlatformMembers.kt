// MODULE: lib
// MODULE_KIND: LibraryBinary
// JVM_TARGET: 11
// FILE: lib.kt
package lib

class Foo {
    inline fun inlineMember() {}

    fun regularMember() {}

    class Nested {
        inline fun inlineNestedMember() {}
    }

    companion object {
        inline fun inlineCompanionMember() {}
    }
}

interface Bar {
    companion object {
        inline fun inlineInterfaceCompanionMember() {}
    }
}

// MODULE: main(lib)
// JVM_TARGET: 1.8
// FILE: main.kt
package main

import lib.*

fun usage(foo: Foo, nested: Foo.Nested) {
    foo.inlineMember()
    foo.regularMember()
    nested.inlineNestedMember()
    Foo.inlineCompanionMember()
    Bar.inlineInterfaceCompanionMember()
}
