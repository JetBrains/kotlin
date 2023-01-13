class Foo {
  suspend fun doSomething(foo: Foo): Bar { TODO() }
}

class Boo {
    private suspend fun doSomething(foo: Foo): Bar { TODO() }
}

class Bar {
  fun <T> async(block: suspend () -> T) {}
}

interface Base {
    suspend fun foo()
}

class Derived: Base {
    override suspend fun foo() { }
}

// WITH_STDLIB
