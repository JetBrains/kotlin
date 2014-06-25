fun main(args: Array<String>) {
    with(A()) {
        println(<selection>prop</selection>)
        println(prop)
    }
}

class A {
    val prop = 1
}

public inline fun <T, R> with(receiver: T, f: T.() -> R): R = receiver.f()