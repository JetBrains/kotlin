package one

interface NonLocalInterface

fun resolv<caret>eMe() {
    open class A : B()
    class B : NonLocalInterface {
        inner class C : A() {

        }
    }
}