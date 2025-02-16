// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt
expect open class <!NO_ACTUAL_FOR_EXPECT{JVM}!>A<!> {
    constructor(s: String)

    constructor(n: Number) : <!EXPECTED_CLASS_CONSTRUCTOR_DELEGATION_CALL, EXPECTED_CLASS_CONSTRUCTOR_DELEGATION_CALL{JVM}!>this<!>("A")
}

expect class <!NO_ACTUAL_FOR_EXPECT{JVM}!>B<!> : A {
    constructor(i: Int)

    constructor() : <!EXPECTED_CLASS_CONSTRUCTOR_DELEGATION_CALL, EXPECTED_CLASS_CONSTRUCTOR_DELEGATION_CALL{JVM}!>super<!>("B")
}

// MODULE: m1-jvm()()(m1-common)
