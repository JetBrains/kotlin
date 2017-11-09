// !LANGUAGE: -OperatorRem
// !DIAGNOSTICS: -UNUSED_PARAMETER

object ModAndRem {
    operator fun mod(x: Int) = 0
    <!UNSUPPORTED_FEATURE!>operator<!> fun rem(x: Int) = ""
}

object ModAssignAndRemAssign {
    operator fun modAssign(x: String) {}
    <!UNSUPPORTED_FEATURE!>operator<!> fun remAssign(x: Int) {}
}

object RemAndModAssign {
    operator fun modAssign(x: Int) {}
    <!UNSUPPORTED_FEATURE!>operator<!> fun rem(x: Int) = RemAndModAssign
}

object OnlyRem {
    <!UNSUPPORTED_FEATURE!>operator<!> fun rem(x: Int) {}
    <!UNSUPPORTED_FEATURE!>operator<!> fun remAssign(x: Int) {}
}

fun test() {
    takeInt(ModAndRem % 1)

    val c = ModAssignAndRemAssign
    c %= ""

    var c1 = RemAndModAssign
    c1 %= 1

    OnlyRem <!UNRESOLVED_REFERENCE!>%<!> 1

    val c2 = OnlyRem
    c2 <!UNRESOLVED_REFERENCE!>%=<!> 1
}

fun takeInt(x: Int) {}