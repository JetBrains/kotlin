data class X(val a: Int, val b: Int)

val (a, b) = X(1, 2)

class A {
    val (c, d) = X(1, 2)

    class Nested {
        val [e, f] = X(1, 2)
    }
}

enum class E {
    ENTRY {
        val (g, h) = X(1, 2)
    };
}

fun foo() {
    class Local {
        val (i, j) = X(1, 2)
    }

    val o = object {
        val (k, l) = X(1, 2)
    }
}
