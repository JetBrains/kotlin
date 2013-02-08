package protectedPack

class Derived(): protectedStaticClass() {
    fun test(): String {
        return protectedStaticClass.Inner().foo()!!
    }
}

fun box(): String {
   return Derived().test()
}
