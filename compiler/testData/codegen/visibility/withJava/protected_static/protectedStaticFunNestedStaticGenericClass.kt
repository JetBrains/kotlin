class Derived(): protectedStaticFunNestedStaticGenericClass<String>.Inner<String>() {
    fun test(): String {
        return protectedStaticFunNestedStaticGenericClass.Inner.protectedFun()!!
    }
}

fun box(): String {
   return Derived().test()
}