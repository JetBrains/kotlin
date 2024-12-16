package org.test

class Outer {
    @RequiresOptIn
    @Retention(AnnotationRetention.BINARY)
    annotation class Nested
}

@Outer.Nested
fun foo() {}
