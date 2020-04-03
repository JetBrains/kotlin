// !WITH_NEW_INFERENCE
// Ambiguity between fun and callable property

open class BaseWithCallableProp {
    val fn = { "fn.invoke()" }

    val bar = { "bar.invoke()"}
    open fun bar(): String = "bar()"
}

interface InterfaceWithFun {
    fun fn(): String = "fn()"
}

class DerivedUsingFun : BaseWithCallableProp(), InterfaceWithFun {
    fun foo(): String =
    <!AMBIGUOUS_SUPER!>super<!>.<!NI;DEBUG_INFO_MISSING_UNRESOLVED, OI;DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>fn<!>()

    override fun bar(): String =
            super.bar()
}