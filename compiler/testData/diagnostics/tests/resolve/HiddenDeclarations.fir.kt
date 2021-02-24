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
    constructor(p: Int) : this("") {}

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
        topLevelFun()
        topLevelProperty++
        "".topLevelExtensionFun()
        "".topLevelExtensionProperty

        memberFun()
        memberProperty
        "".memberExtensionFun()
        "".memberExtensionProperty

        A("")
    }
}

interface I {
    @Deprecated("hidden", level = DeprecationLevel.HIDDEN)
    fun foo1()

    @Deprecated("hidden", level = DeprecationLevel.HIDDEN)
    fun foo2()
}

class X : I {
    override fun foo1() {
    }
}

class B : A("") {
    // still can override it
    override fun memberFun() {
        super.memberFun() // but cannot call super :)
    }
}

class C : A {
    constructor() : super("")
}