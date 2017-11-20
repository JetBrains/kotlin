package test

expect class Foo {
    fun <caret>bar()
}

// REF: [jvm] (in test.Foo).bar()
// REF: [js] (in test.Foo).bar()