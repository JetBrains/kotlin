fun ok() {
    A::a
    A::a + 1
    Map<String, Int>::size

    ::a

    a.b::c
    a::b.c
    a.b::c.d

    a<b>::c
    a<b>::c.d
    a.b<c>::d
    a.b<c>::d.e
    a.b<c.d>::e
    a.b<c.d>::e.d
    a.b<c.d<e.f>>::g.h

    a.b.c<d>.e<f>.g::h

    a::b()
    (a::b)()
    a.(b::c)()
    a.b::c()

    a?::b
    a??::b
    a<b>?::c
    a<b?,c?>?::d

    A::class
    a<b,c>::class
    ::class
    a b ::class
}

fun err0() {
    a::b.c::d
}

fun err1() {
    A::
}

fun err2() {
    A::a::b
}

fun err3() {
    ::
}
