class Derived(): funGenericClass<String>() {
    fun test(): String {
        return funGenericClass.protectedFun()!!
    }
}

fun box(): String {
   return Derived().test()
}
