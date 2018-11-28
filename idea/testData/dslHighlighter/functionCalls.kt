package p

@DslMarker
annotation class A

@DslMarker
annotation class B

@A
fun f() {

}

@A
fun ff() {

}

@B
fun g() {

}

@A
class AC {
    @A
    fun a() = ""
}

fun test() {
    f() // 4

    g() // 1

    f() // 4

    g() // 1

    ff() // 4

    with (A()) {
        a() // 4
    }
}