// for: KT-2170
open class Foo {
    p<caret> val foo = 1
}

// EXIST: private, public, protected
// NUMBER: 3
