// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER, -EXTENSION_SHADOWED_BY_MEMBER

open class Example {
    fun invoke() = 0
    fun get(i: Int) = 0

    fun component1() = 0
    fun component2() = 0

    fun inc() = Example()

    fun plus(o: Example) = 0
}

class Example2 : Example()

operator fun Example.invoke() = ""
operator fun Example.get(i: Int) = ""

operator fun Example.component1() = ""
operator fun Example.component2() = ""

operator fun Example.inc() = Example2()

infix fun Example.plus(o: Example) = ""

fun test() {
    var a = Example()
    val b = Example()

    consumeString(<!NI;TYPE_MISMATCH!><!NI;OPERATOR_MODIFIER_REQUIRED!>a<!>()<!>)
    consumeString(<!NI;OPERATOR_MODIFIER_REQUIRED, NI;TYPE_MISMATCH!>a[1]<!>)

    val (<!NI;OPERATOR_MODIFIER_REQUIRED!>x<!>, <!NI;OPERATOR_MODIFIER_REQUIRED!>y<!>) = Example()
    consumeString(<!NI;TYPE_MISMATCH!>x<!>)
    consumeString(<!NI;TYPE_MISMATCH!>y<!>)

    consumeExample2(<!NI;TYPE_MISMATCH!><!NI;OPERATOR_MODIFIER_REQUIRED!>++<!>a<!>)

    consumeString(a plus b)
}

fun consumeInt(i: Int) {}
fun consumeString(s: String) {}
fun consumeExample2(e: Example2) {}