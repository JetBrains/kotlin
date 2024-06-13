// CHECK_TYPE
// DIAGNOSTICS: -UNUSED_PARAMETER, -UNUSED_VARIABLE

class TestClass {
    inline operator fun <T> invoke(task: () -> T) = task()
}

fun <T> test(value: T, test: TestClass): T {
    val x = test { return value }
    x checkType { _<Nothing>() }

    return value
}

// ---

class Future<T>

interface FutureCallback<E> {
    operator fun <T> invoke(f: (E) -> T): Future<T>
}

fun test(cb: FutureCallback<String>) {
    val a = cb { it[0] }
    a checkType { _<Future<Char>>() }

    val b = cb { it }
    b checkType { _<Future<String>>() }

    val c = cb {}
    c checkType { _<Future<Unit>>() }

    cb.let { callback ->
        val d = callback { it.length }
        d checkType { _<Future<Int>>() }
    }
}