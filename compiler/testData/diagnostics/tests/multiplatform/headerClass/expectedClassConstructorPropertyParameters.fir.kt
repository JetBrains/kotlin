// DIAGNOSTICS: -NO_ACTUAL_FOR_EXPECT
// MODULE: m1-common
// FILE: common.kt
expect class Foo(
        val constructorProperty: String,
        var constructorVar: String,
        constructorParameter: String,
) {
    constructor(<!VAL_OR_VAR_ON_SECONDARY_CONSTRUCTOR_PARAMETER!>val<!> notReported: String)
}

expect annotation class Ann(val s: String)

expect value class ValueClass(val s: String)
