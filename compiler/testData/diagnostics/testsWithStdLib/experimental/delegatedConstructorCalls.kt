// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-76597

@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
annotation class Experimental

open class A @Experimental constructor(x: Int) {
    constructor() : <!OPT_IN_USAGE_ERROR!>this<!>(42) {}
}

class B : <!OPT_IN_USAGE_ERROR!>A<!>(123) {
}

class C : A {
    constructor(value: Int) : <!OPT_IN_USAGE_ERROR!>super<!>(value) {}
}


fun main() {
    <!OPT_IN_USAGE_ERROR!>A<!>(1)
}
