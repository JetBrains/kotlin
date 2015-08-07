enum class E {
    E1,
    
    E2 {
        override fun foo() {}
    };
    
    open fun foo() {}
}
