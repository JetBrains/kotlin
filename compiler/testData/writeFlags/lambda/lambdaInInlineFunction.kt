class Foo {
    inline fun foo() = { }
}

// TESTED_OBJECT_KIND: class
// TESTED_OBJECTS: Foo$foo$1$kotlin_Function0$0
// FLAGS: ACC_PUBLIC, ACC_FINAL, ACC_SUPER
