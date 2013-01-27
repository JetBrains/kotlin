class A: funCallInConstructor(funCallInConstructor.protectedFun()) {
  fun test(): String {
    return protectedProperty!!
  }
}

fun box(): String {
  return A().test()
}
