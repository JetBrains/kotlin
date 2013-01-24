class Derived(): protectedStaticFunGenericClass<String>() {
    fun test(): String {
        return protectedStaticFunGenericClass.protectedFun()!!
    }
}

fun box(): String {
   return Derived().test()
}
