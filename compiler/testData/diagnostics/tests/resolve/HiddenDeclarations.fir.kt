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
        <!UNRESOLVED_REFERENCE!>topLevelFun<!>()
        <!UNRESOLVED_REFERENCE!>topLevelFun<!>(1)
        <!UNRESOLVED_REFERENCE!>topLevelProperty<!><!UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE!>++<!>
        "".<!UNRESOLVED_REFERENCE!>topLevelExtensionFun<!>()
        1.<!UNRESOLVED_REFERENCE!>topLevelExtensionFun<!>()
        "".<!UNRESOLVED_REFERENCE!>topLevelExtensionProperty<!>
        1.<!UNRESOLVED_REFERENCE!>topLevelExtensionProperty<!>

        <!UNRESOLVED_REFERENCE!>memberFun<!>()
        <!UNRESOLVED_REFERENCE!>memberFun<!>(1)
        <!UNRESOLVED_REFERENCE!>privateFun<!>()
        <!UNRESOLVED_REFERENCE!>privateFun<!>(1)
        <!UNRESOLVED_REFERENCE!>memberProperty<!>
        "".<!UNRESOLVED_REFERENCE!>memberExtensionFun<!>()
        1.<!UNRESOLVED_REFERENCE!>memberExtensionFun<!>()
        "".<!UNRESOLVED_REFERENCE!>memberExtensionProperty<!>
        1.<!UNRESOLVED_REFERENCE!>memberExtensionProperty<!>

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
        super.<!UNRESOLVED_REFERENCE!>memberFun<!>() // but cannot call super :)
        <!UNRESOLVED_REFERENCE!>privateFun<!>()
        <!UNRESOLVED_REFERENCE!>privateFun<!>(1)
    }
}

class C : A {
    constructor() : super(<!ARGUMENT_TYPE_MISMATCH!>""<!>)
}
