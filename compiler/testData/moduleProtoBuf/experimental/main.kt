package org.foo

@Experimental
annotation class A

class B {
    @Experimental
    annotation class C
}

@Experimental(Experimental.Level.ERROR, [Experimental.Impact.COMPILATION])
annotation class D
