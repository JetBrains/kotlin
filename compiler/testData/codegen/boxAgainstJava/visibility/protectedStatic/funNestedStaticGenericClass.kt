class Derived(): funNestedStaticGenericClass.Inner<String>() {
    fun test(): String {
        return funNestedStaticGenericClass.Inner.protectedFun()!!
    }
}

fun box(): String {
   return Derived().test()
}
