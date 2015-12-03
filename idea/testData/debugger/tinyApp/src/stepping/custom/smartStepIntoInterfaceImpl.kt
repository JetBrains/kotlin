package smartStepIntoInterfaceImpl

import forTests.MyJavaClass

interface I {
    // without params
    fun foo() {
        val a = 1
    }

    // default param
    fun foo(a: Int, b: Int = 1) {
        val a = 1
    }

    // checkParamNotNull
    fun foo(a: String) {
        val a = 1
    }

    fun fooOverride() {
        val a = 1
    }

    fun staticCallInOverride(s: String) = 1
}

class IImpl: I {
    // super call in override (step into doesn't work)
    override fun fooOverride() {
        return super.fooOverride()
    }

    // java static call
    fun staticCall(s: String): Int {
        return MyJavaClass.staticFun(s)
    }

    // java static call in override
    override fun staticCallInOverride(s: String): Int {
        return MyJavaClass.staticFun(s)
    }
}

class IImpl2: I {
    // java static call in override with this param (step into doesn't work)
    override fun staticCallInOverride(s: String): Int {
        return MyJavaClass.staticFun(this)
    }
}

object Obj: I {
    @JvmStatic private fun staticFun(s: String): Int  {
        return 1
    }

    // kotlin static call
    fun staticCall(s: String): Int {
        return staticFun(s)
    }

    // kotlin static call in override
    override fun staticCallInOverride(s: String): Int {
        return staticFun(s)
    }
}

object Obj2: I {
    @JvmStatic private fun staticFun(s: Obj2): Int  {
        return 1
    }

    // kotlin static call in override with this param (step into doesn't work)
    override fun staticCallInOverride(s: String): Int {
        return staticFun(this)
    }
}

fun barI() = IImpl()

fun main(args: Array<String>) {
    testSmartStepInto()
    testStepInto()
}

fun testSmartStepInto() {
    // TODO First time - doesn't work: should be smartStepIntoInterfaceImpl.kt:8 between smartStepIntoInterfaceImpl.kt:90 and 95
    // SMART_STEP_INTO_BY_INDEX: 2
    //Breakpoint!
    barI().foo()

    // SMART_STEP_INTO_BY_INDEX: 2
    // RESUME: 1
    //Breakpoint!
    barI().foo()

    // SMART_STEP_INTO_BY_INDEX: 2
    // RESUME: 1
    //Breakpoint!
    barI().foo(1)

    // SMART_STEP_INTO_BY_INDEX: 2
    // RESUME: 1
    //Breakpoint!
    barI().foo(1, 2)

    // SMART_STEP_INTO_BY_INDEX: 2
    // RESUME: 1
    //Breakpoint!
    barI().foo("a")
}

fun testStepInto() {
    val ii = IImpl()

    // TODO: should be smartStepIntoInterfaceImpl.kt:31 instead of smartStepIntoInterfaceImpl.kt:22
    // STEP_INTO: 1
    // RESUME: 1
    //Breakpoint!
    ii.fooOverride()

    // STEP_INTO: 1
    // RESUME: 1
    //Breakpoint!
    ii.staticCall("a")

    // STEP_INTO: 1
    // RESUME: 1
    //Breakpoint!
    ii.staticCallInOverride("a")

    val ii2 = IImpl2()

    // TODO: should be smartStepIntoInterfaceImpl.kt:48 instead of MyJavaClass.java:16
    // STEP_INTO: 1
    // RESUME: 1
    //Breakpoint!
    ii2.staticCallInOverride("a")

    // STEP_INTO: 1
    // RESUME: 1
    //Breakpoint!
    Obj.staticCall("a")

    // STEP_INTO: 1
    // RESUME: 1
    //Breakpoint!
    Obj.staticCallInOverride("a")

    // TODO: should be smartStepIntoInterfaceImpl.kt:75 instead of smartStepIntoInterfaceImpl.kt:70
    // STEP_INTO: 1
    // RESUME: 1
    //Breakpoint!
    Obj2.staticCallInOverride("a")
}
