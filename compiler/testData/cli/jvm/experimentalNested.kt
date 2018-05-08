package org.test

class Outer {
    @Experimental
    annotation class Nested
}

@Outer.Nested
fun foo() {}
