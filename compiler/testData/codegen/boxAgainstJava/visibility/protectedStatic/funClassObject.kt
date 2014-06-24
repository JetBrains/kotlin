class A {
  class object: funClassObject() {
    fun test(): String {
      return funClassObject.protectedFun()!!
    }
  }
}

fun box(): String {
  return A.test()
}
