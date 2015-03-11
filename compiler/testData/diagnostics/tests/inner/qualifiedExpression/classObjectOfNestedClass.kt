class Outer {
    class Nested {
        default object {
            fun foo() = 42
        }
    }
    
    default object {
        fun bar() = 239
    }
}

fun foo() = Outer.Nested.foo()
fun bar() = Outer.bar()
