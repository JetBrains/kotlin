// FIR_IDENTICAL
open class B

class A {
    companion object : B() { // Nested B should be invisible here but it's not
        class B
    }
}
