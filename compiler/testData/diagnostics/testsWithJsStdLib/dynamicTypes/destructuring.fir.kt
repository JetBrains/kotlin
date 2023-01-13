// !DIAGNOSTICS: -UNUSED_PARAMETER

fun foo() {
    for ((x, y) in A()) {
        println(x + y)
    }

    bar { (x, y) ->
        println(x + y)
    }

    val x: dynamic = Any()

    val (y, z) = x
    println(y + z)
}

class A {
    operator fun iterator(): Iterator<dynamic> = TODO("")
}

fun bar(f: (dynamic) -> Unit): Unit = TODO("")
