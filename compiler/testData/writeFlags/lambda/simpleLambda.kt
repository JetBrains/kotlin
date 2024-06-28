// LAMBDAS: CLASS

class Foo {
    fun foo() = { }
}

// TESTED_OBJECT_KIND: class
// TESTED_OBJECTS: Foo$foo$1
// FLAGS: ACC_FINAL, ACC_SUPER
