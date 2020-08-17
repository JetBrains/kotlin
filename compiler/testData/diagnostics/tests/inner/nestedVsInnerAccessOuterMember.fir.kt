// NI_EXPECTED_FILE

class Outer {
    fun function() = 42
    val property = ""
    
    class Nested {
        fun f() = <!UNRESOLVED_REFERENCE!>function<!>()
        fun g() = <!UNRESOLVED_REFERENCE!>property<!>
        fun h() = <!UNRESOLVED_LABEL!>this@Outer<!>.<!UNRESOLVED_REFERENCE!>function<!>()
        fun i() = <!UNRESOLVED_LABEL!>this@Outer<!>.<!UNRESOLVED_REFERENCE!>property<!>
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
