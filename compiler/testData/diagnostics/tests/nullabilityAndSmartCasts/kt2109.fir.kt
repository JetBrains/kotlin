//KT-2109 Nullability inference fails in extension function
package kt2109

class A {
    fun foo() {}
}

fun A?.bar() {
    if (this == null) {
        return
    }
    foo()
}

fun A.baz() {
    if (<!SENSELESS_COMPARISON!>this == null<!>) {
        return
    }
    foo()
}
