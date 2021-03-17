// !DIAGNOSTICS: -UNUSED_EXPRESSION

import Obj.ext
import A.Companion.ext2

object Obj {
    val String.ext: String get() = this
}

class A {
    companion object {
        val String.ext2: String get() = this
    }
}

fun test() {
    <!EXTENSION_IN_CLASS_REFERENCE_NOT_ALLOWED!>String::ext<!>
    <!UNRESOLVED_REFERENCE!>Obj::ext<!>

    <!EXTENSION_IN_CLASS_REFERENCE_NOT_ALLOWED!>String::ext2<!>
    <!UNRESOLVED_REFERENCE!>A.Companion::ext2<!>
    <!UNRESOLVED_REFERENCE!>A::ext2<!>

    <!UNRESOLVED_REFERENCE!>A::foo<!>
    <!UNRESOLVED_REFERENCE!>A::bar<!>
}
