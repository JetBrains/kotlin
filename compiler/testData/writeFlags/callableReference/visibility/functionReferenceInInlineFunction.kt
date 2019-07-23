class A {
    fun foo() {}

    inline fun bar() = A::foo
}

// TESTED_OBJECT_KIND: class
// TESTED_OBJECTS: A$bar$1
// FLAGS: ACC_FINAL, ACC_PUBLIC, ACC_SUPER, ACC_SYNTHETIC
