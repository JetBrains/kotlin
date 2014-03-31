package a

open class A {
    protected fun protectedFun(): String = "OK"
}

class BSamePackage: A() {
    fun test(): String {
        val a = {
            protectedFun()
        }
        return a()
    }
}

// 0 INVOKESTATIC a/BSamePackage.protectedFun
