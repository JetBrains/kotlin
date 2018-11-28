// RUNTIME
class A {
    fun fooA() {

    }
}

class B  {
    fun fooB(): Int {
        return 3
    }
}


fun <T> myWith(t: T, init: T.() -> Unit) {

}


fun main() {
    myWith (A()) {
        myWith (B()) {
            foo<caret>
        }
    }
}
// fooB should be first because it's receiver is closer, expected return type is Unit and should be taken into account

// ORDER: fooB, fooA