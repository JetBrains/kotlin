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

    this::class
    super::class
    X()::class
    object {}::class
}

fun expressions() {
    this@x::foo
    super<a>@b::foo

    -a::b
    ++a::b
    a+b::c
    (a+b)::c
    x()::e
    x().y().z()::e

    a::b.c::d
    A::a::b
}

fun emptyLHS() {
    ::x.name
    foo(::x.name)
}
