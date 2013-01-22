import A.B.foo
import A.B.x

class A() {
    class object {
        class B() {
            class object {
                val x = 1
                fun foo() = "2"
            }
        }
    }
}

fun box() = if (x == 1 && foo() == "2") "OK" else "fail"
