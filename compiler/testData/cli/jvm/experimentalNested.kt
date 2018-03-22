package org.test

class Outer {
    @Experimental(Experimental.Level.ERROR, [Experimental.Impact.LINKAGE])
    annotation class Nested
}

@Outer.Nested
fun foo() {}
