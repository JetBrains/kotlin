// NI_EXPECTED_FILE

class Outer {
    fun function() = 42
    val property = ""
    
    class Nested {
        fun f() = function()
        fun g() = property
        fun h() = this@Outer.function()
        fun i() = this@Outer.property
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
