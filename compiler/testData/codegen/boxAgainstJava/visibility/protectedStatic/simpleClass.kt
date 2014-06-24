class Derived(): simpleClass() {
    fun test(): String {
        return simpleClass.Inner().foo()!!
    }
}

fun box(): String {
   return Derived().test()
}
