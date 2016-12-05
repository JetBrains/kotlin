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
    operator fun mod(x: Int): Int = 0
    operator fun rem(x: Int) {}
}

fun test() {
    OldAndNew() % 1
    OnlyOld() % 1
    OnlyNew() % 1
    Sample() % 1

    takeInt(<!TYPE_MISMATCH!>IntAndUnit() % 1<!>)
}

fun takeInt(x: Int) {}