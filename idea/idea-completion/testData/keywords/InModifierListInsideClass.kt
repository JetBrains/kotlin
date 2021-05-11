// FIR_COMPARISON
// for: KT-2170
open class Foo {
    p<caret> val foo = 1
}

// EXIST: private, public, protected
// NOTHING_ELSE
