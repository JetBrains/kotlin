class Derived(): simpleFun() {
    fun test(): String {
        return simpleFun.protectedFun()!!
    }
}

fun box(): String {
   return Derived().test()
}
