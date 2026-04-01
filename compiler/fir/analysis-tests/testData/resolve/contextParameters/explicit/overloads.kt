// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters +ExplicitContextArguments

open class A
open class B : A()
class C : B()

interface I1
interface I2
class Merge : I1, I2

context(_: Boolean, a: String)
fun foo0() = 1

context(_: Boolean)
fun foo0(a: CharSequence) = true


context(_: Any, a: String)
fun foo1() = 1

context(_: Boolean)
fun foo1(a: String) = true


context(_: Any, a: String)
fun foo2() = 1

context(_: Boolean)
fun foo2(b: String) = true


context(_: Any, a: Array<String>)
fun foo3() = 1

context(_: Boolean)
fun foo3(vararg a: String) = true


context(i: I1)
fun foo4() = true

fun foo4(i: I2) = ""


context(a: A)
fun foo5(b: A, i: Int) = true

context(b: A)
fun foo5(i: Int, a: A) = ""


context(a: A)
fun foo6(b: B) = true

context(b: B)
fun foo6(a: A) = ""


context(a: A)
fun foo7(b: B, i: Int) = true

context(b: A)
fun foo7(i: Int, a: B) = ""


context(a: A)
fun foo8(b: B) = true

context(b: B)
<!CONTEXTUAL_OVERLOAD_SHADOWED!>fun foo8(a: B)<!> = ""


context(a: A)
fun B.foo9() = true

context(a: B)
fun A.foo9() = ""


fun interface Runnable {
    fun foo()
}

context(f: Runnable)
fun foo10() = true

fun foo10(f: () -> Unit) = ""


context(a: A, b: B)
<!CONTEXTUAL_OVERLOAD_SHADOWED!>fun foo11()<!> = true

context(a: A, b: A)
fun foo11() = ""


fun test() {
    with(true) {
        val t0 = foo0(a = "")
        <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo1<!>(a = "")
        val t2 = foo2(a = "")
        val t2_ = foo2(b = "")
        val t3 = foo3(a = arrayOf(""))
    }
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo4<!>(i = Merge())
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo5<!>(a = A(), b = A(), i = 42)
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo6<!>(a = A(), b = B())
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo7<!>(a = B(), b = B(), i = 42)
    val t8 = foo8(a = C(), b = C())
    with(B()) {
        <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo9<!>(a = C())
        val t9 = foo9()
        foo9(a = A())
    }
    val t10 = foo10(f = {})
    with(B()) {
        <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo11<!>(a = A())
        val t11 = foo11(b = B())
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionDeclarationWithContext, integerLiteral, lambdaLiteral, stringLiteral */
