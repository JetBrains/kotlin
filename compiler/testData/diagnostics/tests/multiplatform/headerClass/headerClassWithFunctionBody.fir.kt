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

    constructor(s: String) {
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
