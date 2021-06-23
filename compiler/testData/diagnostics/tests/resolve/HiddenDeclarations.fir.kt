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
    val memberProperty = 1

    @Deprecated("hidden", level = DeprecationLevel.HIDDEN)
    fun String.memberExtensionFun(){}

    @Deprecated("hidden", level = DeprecationLevel.HIDDEN)
    val String.memberExtensionProperty: Int get() = 1

    fun foo() {
        <!INVISIBLE_REFERENCE!>topLevelFun<!>()
        <!INVISIBLE_REFERENCE, INVISIBLE_REFERENCE!>topLevelProperty<!>++
        "".<!INVISIBLE_REFERENCE!>topLevelExtensionFun<!>()
        "".<!INVISIBLE_REFERENCE!>topLevelExtensionProperty<!>

        <!INVISIBLE_REFERENCE!>memberFun<!>()
        <!INVISIBLE_REFERENCE!>memberProperty<!>
        "".<!INVISIBLE_REFERENCE!>memberExtensionFun<!>()
        "".<!INVISIBLE_REFERENCE!>memberExtensionProperty<!>

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
    override fun foo1() {
    }
}

class B : A(<!ARGUMENT_TYPE_MISMATCH!>""<!>) {
    // still can override it
    override fun memberFun() {
        super.<!INVISIBLE_REFERENCE!>memberFun<!>() // but cannot call super :)
    }
}

class C : A {
    constructor() : super(<!ARGUMENT_TYPE_MISMATCH!>""<!>)
}
