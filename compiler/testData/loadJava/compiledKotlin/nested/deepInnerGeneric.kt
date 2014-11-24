package test

class A<TA> {
    inner class B<TB> {
        inner class C<TC> {
            inner class D<TD> {
                fun <P1, P2, P3, P4> foo(p1: P1, p2: P2, p3: P3, p4: P4): Nothing = null!!

                fun bar(ta: TA, tb: TB, tc: TC, td: TD): A<TA>.B<TB>.C<TC>.D<TD> = foo<TA, TB, TC, TD>(ta, tb, tc, td)
            }
        }
    }
}
