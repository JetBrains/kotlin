package test

operator fun String.getValue(a: Any, b: Any): String = this

val bigProperty: String = "Hello"

open class Base(s: String) {}

<expr>
class Foo(
    bigProperty: String,
    otherParam: String = test.bigProperty
) : Base(test.bigProperty),
    CharSequence by test.bigProperty
{
    val regularProperty = test.bigProperty
    val delegatedProperty by test.bigProperty
    val lambdaCapture = { test.bigProperty }

    init {
        test.bigProperty()
    }
}
</expr>