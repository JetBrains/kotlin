open class Base<T> {
    public open fun foo(t: T): T = t
    
    public open fun bar() {}
}

class Child: Base<String>() {
    override fun foo(t: String): String {
        return super<Base>.foo(t)
    }
    
    override fun bar() {
        super.bar()
    }
}

// 1 bridge
