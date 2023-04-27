// !WITH_NEW_INFERENCE
// documents inconsistency between scripts and classes, see DeclarationScopeProviderImpl

fun function() = 42
val property = ""

class Nested {
    fun f() = function()
    fun g() = property
}


<!WRONG_MODIFIER_CONTAINING_DECLARATION!>inner<!> class Inner {
    fun innerFun() = function()
    val innerProp = property
    fun innerThisFun() = this<!UNRESOLVED_LABEL!>@NestedInnerClass<!>.function()
    val innerThisProp = this<!UNRESOLVED_LABEL!>@NestedInnerClass<!>.property

    inner class InnerInner {
        fun f() = innerFun()
        fun g() = innerProp
        fun h() = this@Inner.innerFun()
        fun i() = this@Inner.innerProp
    }
}
