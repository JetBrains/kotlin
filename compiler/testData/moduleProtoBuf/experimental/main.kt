package org.foo

@RequiresOptIn
annotation class A

class B {
    @RequiresOptIn
    annotation class C
}

@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
annotation class D
