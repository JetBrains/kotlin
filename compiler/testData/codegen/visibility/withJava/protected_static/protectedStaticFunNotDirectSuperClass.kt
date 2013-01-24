open class A : protectedStaticFunNotDirectSuperClass() {}

class Derived(): A() {
    fun test(): String {
        return protectedStaticFunNotDirectSuperClass.protectedFun()!!
    }
}

fun box(): String {
   return Derived().test()
}

