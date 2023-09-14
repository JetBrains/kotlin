// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER

open class A {
    fun get(index: Int): Char = '*'
}

typealias TA = A

abstract class <!WRONG_MULTIPLE_INHERITANCE!>B<!> : A(), CharSequence
abstract class <!WRONG_MULTIPLE_INHERITANCE!>B2<!> : TA(), CharSequence

interface I {
    fun nextChar(): Char
}

abstract class <!WRONG_MULTIPLE_INHERITANCE!>C<!> : CharIterator(), I {
    override fun nextChar(): Char = '*'
}

class <!WRONG_MULTIPLE_INHERITANCE!>CC(val s: CharSequence)<!> : CharSequence by s, MyCharSequence {}

interface MyCharSequence {
    val length: Int

    operator fun get(index: Int): Char

    fun subSequence(startIndex: Int, endIndex: Int): CharSequence
}