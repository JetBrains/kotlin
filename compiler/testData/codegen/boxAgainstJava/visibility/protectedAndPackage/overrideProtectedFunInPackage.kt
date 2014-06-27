package protectedPackKotlin

import protectedPack.overrideProtectedFunInPackage

class Derived: overrideProtectedFunInPackage() {
    protected override fun foo(): String? {
        return "OK"
    }

    fun test(): String {
        return foo()!!
    }
}

fun box(): String {
   return Derived().test()
}
