package test

// imports should be resolved
import test.topLevelFun
import test.topLevelProperty

@Deprecated("hidden", level = DeprecationLevel.HIDDEN)
fun topLevelFun(){}

@Deprecated("hidden", level = DeprecationLevel.HIDDEN)
var topLevelProperty = 1

@Deprecated("hidden", level = DeprecationLevel.HIDDEN)
fun String.topLevelExtensionFun(){}

@Deprecated("hidden", level = DeprecationLevel.HIDDEN)
val String.topLevelExtensionProperty: Int get() = 1

open class A {
    constructor(p: Int) : this(<!ARGUMENT_TYPE_MISMATCH!>""<!>) {}

    @Deprecated("hidden", level = DeprecationLevel.HIDDEN)
    constructor(s: String){}

    @Deprecated("hidden", level = DeprecationLevel.HIDDEN)
    open fun memberFun(){}

    @Deprecated("hidden", level = DeprecationLevel.HIDDEN)
    private fun privateFun(){}

    @Deprecated("hidden", level = DeprecationLevel.HIDDEN)
    val memberProperty = 1

    @Deprecated("hidden", level = DeprecationLevel.HIDDEN)
    fun String.memberExtensionFun(){}

    @Deprecated("hidden", level = DeprecationLevel.HIDDEN)
    val String.memberExtensionProperty: Int get() = 1

    fun foo() {
        <!DEPRECATION_ERROR!>topLevelFun<!>()
        <!DEPRECATION_ERROR!>topLevelFun<!>(1)
        <!DEPRECATION_ERROR!>topLevelProperty<!><!DEPRECATION_ERROR, UNRESOLVED_REFERENCE!>++<!>
        "".<!DEPRECATION_ERROR!>topLevelExtensionFun<!>()
        1.<!DEPRECATION_ERROR!>topLevelExtensionFun<!>()
        "".<!DEPRECATION_ERROR!>topLevelExtensionProperty<!>
        1.<!DEPRECATION_ERROR!>topLevelExtensionProperty<!>

        <!DEPRECATION_ERROR!>memberFun<!>()
        <!DEPRECATION_ERROR!>memberFun<!>(1)
        <!DEPRECATION_ERROR!>privateFun<!>()
        <!DEPRECATION_ERROR!>privateFun<!>(1)
        <!DEPRECATION_ERROR!>memberProperty<!>
        "".<!DEPRECATION_ERROR!>memberExtensionFun<!>()
        1.<!DEPRECATION_ERROR!>memberExtensionFun<!>()
        "".<!DEPRECATION_ERROR!>memberExtensionProperty<!>
        1.<!DEPRECATION_ERROR!>memberExtensionProperty<!>

        A(<!ARGUMENT_TYPE_MISMATCH!>""<!>)
    }
}

interface I {
    @Deprecated("hidden", level = DeprecationLevel.HIDDEN)
    fun foo1()

    @Deprecated("hidden", level = DeprecationLevel.HIDDEN)
    fun foo2()
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class X<!> : I {
    override fun <!OVERRIDE_DEPRECATION!>foo1<!>() {
    }
}

class B : A(<!ARGUMENT_TYPE_MISMATCH!>""<!>) {
    // still can override it
    override fun <!OVERRIDE_DEPRECATION!>memberFun<!>() {
        super.<!DEPRECATION_ERROR!>memberFun<!>() // but cannot call super :)
        <!DEPRECATION_ERROR!>privateFun<!>()
        <!DEPRECATION_ERROR!>privateFun<!>(1)
    }
}

class C : A {
    constructor() : super(<!ARGUMENT_TYPE_MISMATCH!>""<!>)
}
