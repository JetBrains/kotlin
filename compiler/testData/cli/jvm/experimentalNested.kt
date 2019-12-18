package org.test

class Outer {
    @RequiresOptIn
    annotation class Nested
}

@Outer.Nested
fun foo() {}
