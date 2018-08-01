package p

@DslMarker
annotation class A

@DslMarker
annotation class B

@A
val AC.p1: Int
    get() = 3

@A
var p2: Int = 5

@B
val BC.p3
    get() = 6


@B
var BC.p7
    get() = 3
    set(i) {}

@A
class AC {
    @A
    val a = ""
}

@B
class BC {
    @B
    val b = ""
}

fun test() {
    p2 // 4
    p2 = 6 // 4

    with(AC()) {
        p1 // 4
        a // 4
    }
    with(BC()) {
        p3 // 1
        p7 // 1
        p7 = 3 // 1
        b // 1
    }
}