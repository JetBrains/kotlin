class A
open class B
class C : <!SUPERTYPE_NOT_INITIALIZED!>B<!>


fun bar(a: A) = a
fun bar(b: B) = b

fun foo() {
    val a = A()
    val b = B()
    val c = C()
    val ra = bar(a)
    val rb = bar(b)
    val rc = bar(c)
}
