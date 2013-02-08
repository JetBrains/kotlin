open class A : funNotDirectSuperClass() {}

class Derived(): A() {
    fun test(): String {
        return funNotDirectSuperClass.protectedFun()!!
    }
}

fun box(): String {
   return Derived().test()
}

