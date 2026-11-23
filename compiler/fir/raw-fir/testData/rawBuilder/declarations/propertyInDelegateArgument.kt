fun foo(a: Any): Lazy<Int> = lazy { 1 }

class C {
    val annotated by foo(@Suppress("") val x by lazy { 1 })
    val binary by foo(1 + val y = 2)
}

val topLevel by foo(1 + val z = 3)
