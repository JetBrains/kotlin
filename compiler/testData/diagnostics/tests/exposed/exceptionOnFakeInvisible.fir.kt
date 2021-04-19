// KT-14469: SOE during effective visibility evaluation

abstract class Base(private val v: String)

fun bar(arg: String) = arg

class Derived : Base("123") {

    private inline fun foo() {
        bar(<!INVISIBLE_REFERENCE!>v<!>)
    }
}
