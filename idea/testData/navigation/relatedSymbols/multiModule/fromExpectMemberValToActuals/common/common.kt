package test

expect class Foo {
    val <caret>bar: Int
}

// REF: [jvm] (in test.Foo).bar
// REF: [js] (in test.Foo).bar