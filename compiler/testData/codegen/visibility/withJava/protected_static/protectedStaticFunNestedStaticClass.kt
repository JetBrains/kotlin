class Derived(): protectedStaticFunNestedStaticClass.Inner() {
    fun test(): String {
        return protectedStaticFunNestedStaticClass.Inner.protectedFun()!!
    }
}

fun box(): String {
   return Derived().test()
}
