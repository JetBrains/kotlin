enum class Foo {
    A {
        fun foo() {}
    }
}

// TESTED_OBJECT_KIND: class
// TESTED_OBJECTS: Foo$A
// FLAGS: ACC_FINAL, ACC_SUPER
