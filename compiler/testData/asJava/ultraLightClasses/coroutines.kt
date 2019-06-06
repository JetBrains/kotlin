class Foo {
  suspend fun doSomething(foo: Foo): Bar {}
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
