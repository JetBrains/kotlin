class Outer {
    class Nested {
        companion object {
            fun foo() = 42
        }
    }
    
    companion object {
        fun bar() = 239
    }
}

fun foo() = Outer.Nested.foo()
fun bar() = Outer.bar()
