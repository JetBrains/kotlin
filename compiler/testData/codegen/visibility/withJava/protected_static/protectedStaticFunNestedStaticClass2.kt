class Derived(): protectedStaticFunNestedStaticClass2.A.B() {
    fun test(): String {
        return protectedStaticFunNestedStaticClass2.A.B.protectedFun()!!
    }
}

fun box(): String {
   return Derived().test()
}
