open class A {
    val a = 1
}

interface T {
    val t: Int
}

class B : A(), T {
    override val t: Int = 2
}

fun foo(o: Any) {
    val x = when (o) {
        is A -> {
            if (o is T) <selection>o.a + o.t</selection> else o.a
        }
        else -> o.hashCode()
    }
}