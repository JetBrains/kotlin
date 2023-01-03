// !DIAGNOSTICS: -UNUSED_PARAMETER

open class A {
    fun get(index: Int): Char = '*'
}

abstract class B : A(), CharSequence

interface I {
    fun nextChar(): Char
}

abstract class C : CharIterator(), I {
    override fun nextChar(): Char = '*'
}

class CC(val s: CharSequence) : CharSequence by s, MyCharSequence {}

interface MyCharSequence {
    val length: Int

    operator fun get(index: Int): Char

    fun subSequence(startIndex: Int, endIndex: Int): CharSequence
}
