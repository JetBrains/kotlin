open class Foo {
    p<caret> val foo = 1
}

// FOR: KT-2170
// EXIST: private, public, protected