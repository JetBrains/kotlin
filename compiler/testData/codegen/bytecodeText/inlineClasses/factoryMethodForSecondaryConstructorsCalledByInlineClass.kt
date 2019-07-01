// !LANGUAGE: +InlineClasses

// FILE: Z.kt
inline class Z(val x: Int) {
    constructor(x: Long) : this(x.toInt())
    constructor(s: String) : this(s.length)
    constructor(a: Int, b: Int) : this(a + b)
}

// FILE: test.kt
fun test1() = Z(0L)
fun test2() = Z("abcdef")
fun test3() = Z(1, 2)

// @TestKt.class:
// 0 INVOKESTATIC Z\$Erased\.constructor
// 0 INVOKESTATIC Z\-Erased\.constructor
// 1 INVOKESTATIC Z\.constructor-impl \(J\)I
// 1 INVOKESTATIC Z\.constructor-impl \(Ljava/lang/String;\)I
// 1 INVOKESTATIC Z\.constructor-impl \(II\)I