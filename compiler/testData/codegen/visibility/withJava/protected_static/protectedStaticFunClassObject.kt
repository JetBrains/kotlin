class A {
  class object: protectedStaticFunClassObject() {
    fun test(): String {
      return protectedStaticFunClassObject.protectedFun()!!
    }
  }
}

fun box(): String {
  return A.test()
}
