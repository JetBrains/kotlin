class A(val aa: A?)

fun f(a: A) {
    val x = a?.a<caret>a
}