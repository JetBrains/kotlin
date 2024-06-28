// ISSUE: KT-61101

open class A {
    var x: Int = 0
        private set
}

class B : A()

fun test() {
    val b = B()
    b.<!INVISIBLE_SETTER!>x<!> = throw Exception()
}
