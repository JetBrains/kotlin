package a

import a.Outer.Nested.*
import a.Outer.Inner.*

class Outer {
    class Nested {
        class NN {
        }
        class NN2 {
        }
        inner class NI {
        }
        inner class NI2 {
        }
    }

    inner class Inner {
        inner class II {
        }
        inner class II2 {
        }
    }
}

fun <T> with(v: T, body: T.() -> Unit) = v.body()

<selection>fun f(p1: NN, p2: NI, p3: II) {
    NN2()
    with(Outer.Nested()) {
        NI2()
    }
    with(Outer().Inner()) {
        II2()
    }
}</selection>