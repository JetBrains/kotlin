/** should load cls */
class Foo {
  suspend fun doSomething(foo: Foo): Bar {}
}

/** should load cls */
class Bar {
  fun <T> async(block: suspend () -> T)
}

/** should load cls */
interface Base {
    suspend fun foo()
}

/** should load cls */
class Derived: Base {
    override suspend fun foo() { ... }
}
