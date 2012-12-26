class Outer {
    class Nested {
        class object {
            fun foo() = 42
        }
    }
    
    class object {
        fun bar() = 239
    }
}

fun foo() = Outer.Nested.foo()
fun bar() = Outer.bar()
