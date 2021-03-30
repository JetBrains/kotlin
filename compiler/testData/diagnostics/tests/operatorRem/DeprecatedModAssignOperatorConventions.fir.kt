// !LANGUAGE: -ProhibitOperatorMod
// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

class OldAndNew {
    operator fun modAssign(x: Int) {}
    operator fun remAssign(x: Int) {}
}

class OnlyOld {
    operator fun modAssign(x: Int) {}
}

class OnlyNew {
    operator fun remAssign(x: Int) {}
}

class Sample

operator fun Sample.modAssign(x: Int) {}
operator fun Sample.remAssign(x: Int) {}

class ModAndRemAssign {
    operator fun mod(x: Int): ModAndRemAssign = ModAndRemAssign()
    operator fun remAssign(x: Int) {}
}

fun test() {
    val oldAndNew = OldAndNew()
    oldAndNew %= 1

    val onlyOld = OnlyOld()
    onlyOld <!INAPPLICABLE_CANDIDATE!>%=<!> 1

    val onlyNew = OnlyNew()
    onlyNew %= 1

    val sample = Sample()
    sample %= 1

    var modAndRemAssign = ModAndRemAssign()
    modAndRemAssign %= 1
}
