trait A {
    fun foo() {
    }

    fun bar() {
    }
}

class UnnecessaryTypeParameter() : A {
    override fun foo() {
        super<A>.foo() // should warn
    }

    override fun bar() {
        super<A>@UnnecessaryTypeParameter.bar() // should warn
    }
}
