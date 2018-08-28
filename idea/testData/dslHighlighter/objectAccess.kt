package p

import p.D.*
import p.C.*

@DslMarker
annotation class A

@DslMarker
annotation class B

@A
class C {
    @A
    object C1

    @A
    object C2

    class C3 {
        @A
        companion object
    }
}

@B
class D {
    @B
    object D1

    class D2 {
        @B
        companion object
    }
}

fun test() {
    with(C()) {
        C1 // 4
        C2 // 4
        C3 // 4
        C3.Companion // 4
    }
    with(D()) {
        D1 // 1
        D2 // 1
        D2.Companion // 1
    }
}