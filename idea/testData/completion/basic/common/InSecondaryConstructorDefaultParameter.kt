fun topLevel() = 1

class A(val prop: Int, arg: Int) {
    val another = 1
    constructor(abc: Int = <caret>): this(1, 1) {
        val local = 1
    }

    fun foo() = 1
}

// EXIST: topLevel, abc
// ABSENT: arg, local, prop, another, foo
