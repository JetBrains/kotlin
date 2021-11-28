// !LANGUAGE: -ProhibitOperatorMod
// !DIAGNOSTICS: -UNUSED_PARAMETER

class OldAndNew {
    operator fun mod(x: Int) {}
    operator fun rem(x: Int) {}
}

class OnlyOld {
    operator fun mod(x: Int) {}
}

class OnlyNew {
    operator fun rem(x: Int) {}
}

class Sample

operator fun Sample.mod(x: Int) {}
operator fun Sample.rem(x: Int) {}

class IntAndUnit {
    operator fun mod(x: Int) = 0
    operator fun rem(x: Int): Int = 0
}

fun test() {
    OldAndNew() % 1
    OnlyOld() <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>%<!> 1
    OnlyNew() % 1
    Sample() % 1

    takeInt(IntAndUnit() % 1)
}

fun takeInt(x: Int) {}
