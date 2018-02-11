package test

expect class Foo {
    class <caret>Bar
}

// REF: [js] (in test.Foo).Bar
// REF: [jvm] (in test.Foo).Bar