package to

import a.with
import a.Outer

fun f(p1: Outer.Nested.NN, p2: Outer.Nested.NI, p3: Outer.Inner.IN, p4: Outer.Inner.II) {
    Outer.Nested.NN2()
    with(Outer.Nested()) {
        NI2()
    }
    Outer.Inner.IN2()
    with(Outer().Inner()) {
        II2()
    }
}