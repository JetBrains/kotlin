package to

import a.Outer.Nested.NN
import a.Outer.Nested.NI
import a.Outer.Inner.IN
import a.Outer.Inner.II
import a.Outer.Nested.NN2
import a.with
import a.Outer
import a.Outer.Inner.IN2

fun f(p1: NN, p2: NI, p3: IN, p4: II) {
    NN2()
    with(Outer.Nested()) {
        NI2()
    }
    IN2()
    with(Outer().Inner()) {
        II2()
    }
}