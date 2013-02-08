class Derived(): funNestedStaticClass.Inner() {
    fun test(): String {
        return funNestedStaticClass.Inner.protectedFun()!!
    }
}

fun box(): String {
   return Derived().test()
}
