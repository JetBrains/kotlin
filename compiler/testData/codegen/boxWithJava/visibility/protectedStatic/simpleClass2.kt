class Derived(): simpleClass2.A() {
    fun test(): String {
        return simpleClass2.A.B().foo()!!
    }
}

fun box(): String {
   return Derived().test()
}
