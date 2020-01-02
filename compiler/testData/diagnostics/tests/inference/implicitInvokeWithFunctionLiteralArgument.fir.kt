// !WITH_NEW_INFERENCE
// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_PARAMETER, -UNUSED_VARIABLE

class TestClass {
    inline operator fun <T> invoke(task: () -> T) = task()
}

fun <T> test(value: T, test: TestClass): T {
    val x = test { return value }
    x checkType { <!UNRESOLVED_REFERENCE!>_<!><Nothing>() }

    return value
}

// ---

class Future<T>

interface FutureCallback<E> {
    operator fun <T> invoke(f: (E) -> T): Future<T>
}

fun test(cb: FutureCallback<String>) {
    val a = cb { it[0] }
    a checkType { <!UNRESOLVED_REFERENCE!>_<!><Future<Char>>() }

    val b = cb { it }
    b checkType { <!UNRESOLVED_REFERENCE!>_<!><Future<String>>() }

    val c = cb {}
    c checkType { <!UNRESOLVED_REFERENCE!>_<!><Future<Unit>>() }

    cb.let { callback ->
        val d = callback { it.length }
        d checkType { _<Future<Int>>() }
    }
}