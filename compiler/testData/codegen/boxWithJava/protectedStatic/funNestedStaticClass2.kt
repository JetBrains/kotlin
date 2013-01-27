class Derived(): funNestedStaticClass2.A.B() {
    fun test(): String {
        return funNestedStaticClass2.A.B.protectedFun()!!
    }
}

fun box(): String {
   return Derived().test()
}
