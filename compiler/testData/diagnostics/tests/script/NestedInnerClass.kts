// documents inconsistency between scripts and classes, see DeclarationScopeProviderImpl

fun function() = 42
val property = ""

class Nested {
    fun f() = function()
    fun g() = property
}


inner class Inner {
    fun innerFun() = function()
    val innerProp = property
    fun innerThisFun() = this@NestedInnerClass.function()
    val innerThisProp = this@NestedInnerClass.property

    inner class InnerInner {
        fun f() = innerFun()
        fun g() = innerProp
        fun h() = this@Inner.innerFun()
        fun i() = this@Inner.innerProp
    }
}
