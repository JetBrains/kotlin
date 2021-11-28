package a

open class A {
    protected fun protectedFun(): String = "OK"
}

class BSamePackage: A() {
    // known to only be called within `BSamePackage`, so accessors are redundant
    private inline fun test(): String = protectedFun()

    fun onlyTestCallSite() = test()
}

// JVM_TEMPLATES
// 2 INVOKESTATIC a/BSamePackage.access
// JVM_IR_TEMPLATES
// 0 INVOKESTATIC a/BSamePackage.access
