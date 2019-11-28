enum class E {
    E1 {
        override fun foo() = outerFun() + super.outerFun()
    },
    E2 {
        override fun foo() = E1.foo()
    };
    
    abstract fun foo(): Int
    
    fun outerFun() = 42
}
