class A {
    default object {
    }
}

class B {
    default object Named {
    }
}

fun main(args: Array<String>) {
    val a = A
    B.Named
}