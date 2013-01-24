class Derived(): protectedStaticClass2.A() {
    fun test(): String {
        return protectedStaticClass2.A.B().foo()!!
    }
}

fun box(): String {
   return Derived().test()
}
