package foo

annotation class Test

object Foo {
    private annotation class Test
}

@Test fun foo()

// ANNOTATION: foo.Test
// SEARCH: method:foo
// OPTIMIZED_TRUE: 1
