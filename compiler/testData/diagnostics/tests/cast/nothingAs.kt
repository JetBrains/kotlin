// !DIAGNOSTICS: -UNREACHABLE_CODE

fun TODO(): Nothing = throw java.lang.IllegalStateException()

open class OpenClass
class FinalClass : OpenClass()
abstract class AbstractClass
interface Interface

fun test() {
    TODO() <!USELESS_CAST!>as Any<!>
    TODO() <!USELESS_CAST!>as Any?<!>
    TODO() <!USELESS_CAST!>as OpenClass<!>
    TODO() <!USELESS_CAST!>as FinalClass<!>
    TODO() <!USELESS_CAST!>as AbstractClass<!>
    TODO() <!USELESS_CAST!>as Interface<!>

    val a = TODO() as Any
    val b = TODO() as Any?
    val c = TODO() as OpenClass
    val d = TODO() as FinalClass
    val e = TODO() as AbstractClass
    val f = TODO() as Interface
}

fun a() = TODO() <!USELESS_CAST!>as Any<!>
fun b() = TODO() <!USELESS_CAST!>as Any?<!>
fun c() = TODO() <!USELESS_CAST!>as OpenClass<!>
fun d() = TODO() <!USELESS_CAST!>as FinalClass<!>
fun e() = TODO() <!USELESS_CAST!>as AbstractClass<!>
fun f() = TODO() <!USELESS_CAST!>as Interface<!>
