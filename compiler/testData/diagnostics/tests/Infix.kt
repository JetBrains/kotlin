// FIR_IDENTICAL
class Pair<out A, out B>(val first: A, val second: B)

class Example {
    infix fun to(other: Example) = Pair(this, other)
    fun toNonInfix(other: Example) = Pair(this, other)
}

infix fun Example.toExt(other: Example) = Pair(this, other)
fun Example.toExtNonInfix(other: Example) = Pair(this, other)

<!INAPPLICABLE_INFIX_MODIFIER!>infix<!> fun Example.toExtWithExtraParams(other: Example, blah: Int = 0) = Pair(this, other)
fun Example.toExtNonInfixWithExtraParams(other: Example, blah: Int = 0) = Pair(this, other)

<!INAPPLICABLE_INFIX_MODIFIER!>infix<!> fun Example.toExtDefaultValues(other: Example? = null, blah: Int = 0) = Pair(this, other)
fun Example.toExtNonInfixDefaultValues(other: Example? = null, blah: Int = 0) = Pair(this, other)

fun Example.withLambda(f: () -> Unit) = Pair(this, f)

fun test() {
    Example() to Example()
    Example() <!INFIX_MODIFIER_REQUIRED!>toNonInfix<!> Example()
    Example().toNonInfix(Example())

    val a = Example()
    val b = Example()

    a toExt b
    a <!INFIX_MODIFIER_REQUIRED!>toExtNonInfix<!> b
    a.toExtNonInfix(b)

    a toExtWithExtraParams b
    a <!INFIX_MODIFIER_REQUIRED!>toExtNonInfixWithExtraParams<!> b

    a toExtDefaultValues b
    a <!INFIX_MODIFIER_REQUIRED!>toExtNonInfixDefaultValues<!> b

    a <!INFIX_MODIFIER_REQUIRED!>withLambda<!> { }
}
