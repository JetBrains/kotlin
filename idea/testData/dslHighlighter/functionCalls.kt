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

fun test() {
    f() // 4

    g() // 1

    f() // 4

    g() // 1

    ff() // 4
}