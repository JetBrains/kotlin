class Derived(): protectedStaticFun() {
    fun test(): String {
        return protectedStaticFun.protectedFun()!!
    }
}

fun box(): String {
   return Derived().test()
}
