// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters +ExplicitContextArguments

open class A
open class B : A()
class C : B()

interface I1
interface I2
class Merge : I1, I2

class Box<out T>


context(a: T)
fun <T : B> foo0a() = true

context(a: A)
fun foo0a() = ""

fun <T : B> foo0b(a: T) = true

context(a: A)
fun foo0b() = ""

context(a: T)
fun <T : B> foo0c() = true

fun foo0c(a: A) = ""


context(a: T)
<!CONTEXTUAL_OVERLOAD_SHADOWED!>fun <T : B> foo1a()<!> = true

context(a: T)
fun <T> foo1a() = ""

fun <T : B> foo1b(a: T) = true

context(a: T)
fun <T> foo1b() = ""

context(a: T)
fun <T : B> foo1c() = true

fun <T> foo1c(a: T) = ""


context(a: T)
fun <T> foo2a() = true

context(a: A)
fun foo2a() = ""

fun <T> foo2b(a: T) = true

context(a: A)
fun foo2b() = ""

context(a: T)
fun <T> foo2c() = true

fun foo2c(a: A) = ""


context(a: T)
fun <T : I1> foo3a() = true

context(a: T)
fun <T : I2> foo3a() = ""

fun <T : I1> foo3b(a: T) = true

context(a: T)
fun <T : I2> foo3b() = ""

context(a: T)
fun <T : I1> foo3c() = true

fun <T : I2> foo3c(a: T) = ""


context(a: Box<T>)
fun <T : B> foo4a() = true

context(a: Box<A>)
fun foo4a() = ""

fun <T : B> foo4b(a: Box<T>) = true

context(a: Box<A>)
fun foo4b() = ""

context(a: Box<T>)
fun <T : B> foo4c() = true

fun foo4c(a: Box<A>) = ""


fun test() {
    val t0a = foo0a(a = C())
    val t0a_ = foo0a(a = A())
    val t0b = foo0b(a = C())
    val t0b_ = foo0b(a = A())
    val t0c = foo0c(a = C())
    val t0c_ = foo0c(a = A())

    val t1a = foo1a(a = C())
    val t1a_ = foo1a(a = A())
    val t1b = foo1b(a = C())
    val t1b_ = foo1b(a = A())
    val t1c = foo1c(a = C())
    val t1c_ = foo1c(a = A())

    val t2a = foo2a(a = C())
    val t2a_ = foo2a(a = A())
    val t2b = foo2b(a = C())
    val t2b_ = foo2b(a = A())
    val t2c = foo2c(a = C())
    val t2c_ = foo2c(a = A())

    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo3a<!>(a = Merge())
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo3b<!>(a = Merge())
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo3c<!>(a = Merge())

    val t4a = foo4a(a = Box<C>())
    val t4a_ = foo4a(a = Box<A>())
    val t4b = foo4b(a = Box<C>())
    val t4b_ = foo4b(a = Box<A>())
    val t4c = foo4c(a = Box<C>())
    val t4c_ = foo4c(a = Box<A>())
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionDeclarationWithContext, integerLiteral, lambdaLiteral, stringLiteral */
