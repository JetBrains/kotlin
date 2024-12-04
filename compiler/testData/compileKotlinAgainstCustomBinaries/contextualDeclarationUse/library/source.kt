package test

context(_: Int) val p get() = 42

class A {
    context(_: Int) val p get() = 42

    context(_: Int) fun m() {}
}

context(s: String)
fun f() {
    println(s)
}

fun ordinary() {
    println("I do not require context receivers!")
}