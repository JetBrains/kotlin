// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt
expect class Foo(
        val constructorProperty: String,
        constructorParameter: String
) {
    init {
        "no"
    }

    <!PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED!>constructor(s: String)<!> {
        "no"
    }

    constructor() : this("no")

    val prop: String = "no"

    var getSet: String
        get() = "no"
        set(value) {}

    fun functionWithBody(x: Int): Int {
        return x + 1
    }
}
