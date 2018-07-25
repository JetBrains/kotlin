// !LANGUAGE: -ProhibitOperatorMod
// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

class OldAndNew {
    <!DEPRECATED_BINARY_MOD!>operator<!> fun mod(x: Int) {}
    operator fun rem(x: Int) {}
}

class OnlyOld {
    <!DEPRECATED_BINARY_MOD!>operator<!> fun mod(x: Int) {}
}

class OnlyNew {
    operator fun rem(x: Int) {}
}

class Sample

<!DEPRECATED_BINARY_MOD!>operator<!> fun Sample.mod(x: Int) {}
operator fun Sample.rem(x: Int) {}

class IntAndUnit {
    <!DEPRECATED_BINARY_MOD!>operator<!> fun mod(x: Int) = 0
    operator fun rem(x: Int): Int = 0
}

fun test() {
    OldAndNew() % 1
    OnlyOld() <!OI;DEPRECATED_BINARY_MOD_AS_REM!>%<!> 1
    OnlyNew() % 1
    Sample() % 1

    takeInt(IntAndUnit() % 1)
}

fun takeInt(x: Int) {}