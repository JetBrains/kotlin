//RELEASE_COROUTINE_NEEDED
//CHECK_BY_JAVA_FILE
class Foo {
  suspend fun doSomething(foo: Foo): Bar {}
}

class Boo {
    private suspend fun doSomething(foo: Foo): Bar {}
}

class Bar {
  fun <T> async(block: suspend () -> T)
}

interface Base {
    suspend fun foo()
}

class Derived: Base {
    override suspend fun foo() { ... }
}

// FIR_COMPARISON
