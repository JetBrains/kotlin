// !LANGUAGE: +InlineClasses
// !DIAGNOSTICS: -UNUSED_PARAMETER

package kotlin

annotation class JvmInline

interface A {
    val goodSize: Int
}

interface B {
    val badSize: Int
}

@JvmInline
value class Foo(val x: Int) : A, B {
    val a0
        get() = 0

    val a1 = 0

    var a2: Int
        get() = 1
        set(value) {}

    var a3: Int = 0
        get() = 1
        set(value) {
            field = value
        }

    override val goodSize: Int
        get() = 0

    override val badSize: Int = 0

    lateinit var lateinitProperty: String
}
