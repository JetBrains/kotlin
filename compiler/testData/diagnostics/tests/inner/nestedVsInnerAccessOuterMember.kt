class Outer {
    fun function() = 42
    val property = ""
    
    class Nested {
        fun f() = <!INACCESSIBLE_OUTER_CLASS_EXPRESSION!>function()<!>
        fun g() = <!INACCESSIBLE_OUTER_CLASS_EXPRESSION!>property<!>
        fun h() = this<!INACCESSIBLE_OUTER_CLASS_EXPRESSION!>@Outer<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>function<!>()
        fun i() = this<!INACCESSIBLE_OUTER_CLASS_EXPRESSION!>@Outer<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>property<!>
    }
    
    inner class Inner {
        fun innerFun() = function()
        val innerProp = property
        fun innerThisFun() = this@Outer.function()
        val innerThisProp = this@Outer.property
        
        inner class InnerInner {
            fun f() = innerFun()
            fun g() = innerProp
            fun h() = this@Inner.innerFun()
            fun i() = this@Inner.innerProp
        }
    }
}
