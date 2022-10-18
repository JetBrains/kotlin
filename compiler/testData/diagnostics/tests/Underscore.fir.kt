// !DIAGNOSTICS: -DEPRECATION -TOPLEVEL_TYPEALIASES_ONLY

import kotlin.Deprecated as <!UNDERSCORE_IS_RESERVED!>___<!>

@<!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>___<!>("") data class Pair(val x: Int, val y: Int)

class <!UNDERSCORE_IS_RESERVED!>_<!><<!UNDERSCORE_IS_RESERVED!>________<!>>
val <!UNDERSCORE_IS_RESERVED!>______<!> = <!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>_<!><Int>()

fun <!UNDERSCORE_IS_RESERVED!>__<!>(<!UNDERSCORE_IS_RESERVED!>___<!>: Int, y: <!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>_<!><Int>?): Int {
    val (_, z) = Pair(<!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>___<!> - 1, 42)
    val (x, <!UNDERSCORE_IS_RESERVED!>__________<!>) = Pair(<!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>___<!> - 1, 42)
    val <!UNDERSCORE_IS_RESERVED!>____<!> = x
    // in backquotes: allowed
    val <!REDECLARATION!>`_`<!> = <!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>__________<!>

    val q = fun(_: Int, <!UNDERSCORE_IS_RESERVED!>__<!>: Int) {}
    q(1, 2)

    val <!REDECLARATION, UNDERSCORE_IS_RESERVED!>_<!> = 56

    fun localFun(<!UNDERSCORE_IS_RESERVED!>_<!>: String) = 1

    <!UNDERSCORE_IS_RESERVED!>__<!>@ return if (y != null) <!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>__<!>(<!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>____<!>, y) else <!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>__<!>(`_`, <!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>______<!>)
}


class A1(val <!UNDERSCORE_IS_RESERVED!>_<!>: String)
class A2(<!UNDERSCORE_IS_RESERVED!>_<!>: String) {
    class B {
        typealias <!UNDERSCORE_IS_RESERVED!>_<!> = CharSequence
    }
    val <!UNDERSCORE_IS_RESERVED!>_<!>: Int = 1

    fun <!UNDERSCORE_IS_RESERVED!>_<!>() {}

    fun foo(<!UNDERSCORE_IS_RESERVED!>_<!>: Double) {}
}

// one underscore parameters for named function are still prohibited
fun oneUnderscore(<!UNDERSCORE_IS_RESERVED!>_<!>: Int) {}

fun doIt(f: (Any?) -> Any?) = f(null)

val something = doIt { <!UNDERSCORE_IS_RESERVED!>__<!> -> <!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>__<!> }
val something2 = doIt { _ -> 1 }

var p: Int?
    get() = null
    set(_) {}

object `____` {
    object Nested {
        fun method() {}
    }
}

fun test() {
    <!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>____<!>.Nested.method()
}
