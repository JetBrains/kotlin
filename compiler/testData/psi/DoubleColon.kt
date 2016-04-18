fun simple() {
    A::a
    A::a + 1
    Map<String, Int>::size

    ::a

    a.b::c
    a::b.c
    a.b::c.d

    (a::b)()
    a.(b::c)()
}

fun genericType() {
    a<b>::c
    a<b>::c.d
    a.b<c>::d
    a.b<c>::d.e
    a.b<c.d>::e
    a.b<c.d>::e.d
    a.b<c.d<e.f>>::g.h

    a.b.c<d>.e<f>.g::h
}

fun nullableType() {
    a?::b
    a??::b
    a<b>?::c
    a<b?,c?>?::d
}

fun classLiteral() {
    A::class
    a<b,c>::class
    ::class
    a b ::class
}
