// !LANGUAGE: -OperatorRem
// !DIAGNOSTICS: -UNUSED_PARAMETER

object ModAndRem {
    operator fun mod(x: Int) = 0
    operator fun rem(x: Int) = ""
}

object ModAssignAndRemAssign {
    operator fun modAssign(x: String) {}
    operator fun remAssign(x: Int) {}
}

object RemAndModAssign {
    operator fun modAssign(x: Int) {}
    operator fun rem(x: Int) = RemAndModAssign
}

object OnlyRem {
    operator fun rem(x: Int) {}
    operator fun remAssign(x: Int) {}
}

fun test() {
    takeInt(<!ARGUMENT_TYPE_MISMATCH!>ModAndRem % 1<!>)

    val c = ModAssignAndRemAssign
    c %= <!ARGUMENT_TYPE_MISMATCH!>""<!>

    var c1 = RemAndModAssign
    c1 %= 1

    OnlyRem % 1

    val c2 = OnlyRem
    c2 %= 1
}

fun takeInt(x: Int) {}