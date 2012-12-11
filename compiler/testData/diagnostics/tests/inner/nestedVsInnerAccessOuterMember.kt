class Outer {
    fun function() = 42
    val property = ""
    
    class Nested {
        fun f() = <!INACCESSIBLE_OUTER_CLASS_EXPRESSION!>function()<!>
        fun g() = <!INACCESSIBLE_OUTER_CLASS_EXPRESSION!>property<!>
    }
    
    inner class Inner {
        fun innerFun() = function()
        val innerProp = property
        
        inner class InnerInner {
            fun f() = innerFun()
            fun g() = innerProp
        }
    }
}
