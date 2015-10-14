// !DIAGNOSTICS: -UNUSED_EXPRESSION

import Obj.ext
import A.Companion.ext2

object Obj {
    fun foo() {}
    val bar = 2
    val String.ext: String get() = this
}

class A {
    companion object {
        fun foo() {}
        val bar = 2
        val String.ext2: String get() = this
    }
}

fun test() {
    Obj::<!CALLABLE_REFERENCE_TO_OBJECT_MEMBER!>foo<!>
    Obj::<!CALLABLE_REFERENCE_TO_OBJECT_MEMBER!>bar<!>
    String::<!CALLABLE_REFERENCE_TO_OBJECT_MEMBER!>ext<!>

    A.Companion::<!CALLABLE_REFERENCE_TO_OBJECT_MEMBER!>foo<!>
    A.Companion::<!CALLABLE_REFERENCE_TO_OBJECT_MEMBER!>bar<!>
    String::<!CALLABLE_REFERENCE_TO_OBJECT_MEMBER!>ext2<!>

    A::<!UNRESOLVED_REFERENCE!>foo<!>
    A::<!UNRESOLVED_REFERENCE!>bar<!>
}
