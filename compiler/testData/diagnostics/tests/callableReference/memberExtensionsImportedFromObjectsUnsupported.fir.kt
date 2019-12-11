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
    String::ext
    Obj::ext

    String::ext2
    A.Companion::ext2
    A::ext2

    A::foo
    A::bar
}
