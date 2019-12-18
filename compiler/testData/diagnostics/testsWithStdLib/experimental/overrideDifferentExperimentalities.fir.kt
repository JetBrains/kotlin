// !USE_EXPERIMENTAL: kotlin.RequiresOptIn

@RequiresOptIn(RequiresOptIn.Level.WARNING)
annotation class E1
@RequiresOptIn(RequiresOptIn.Level.WARNING)
annotation class E3

interface Base1 {
    @E1
    fun foo()
}

interface Base2 {
    fun foo()
}

interface Base3 {
    @E3
    fun foo()
}

class DerivedA : Base1, Base2, Base3 {
    override fun foo() {}
}

class DerivedB : Base1, Base3 {
    @E3
    override fun foo() {}
}

class DerivedC : Base1, Base2, Base3 {
    @E1
    @E3
    override fun foo() {}
}
