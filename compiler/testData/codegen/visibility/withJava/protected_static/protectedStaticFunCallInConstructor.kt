class A: protectedStaticFunCallInConstructor(protectedStaticFunCallInConstructor.protectedFun()) {
  fun test(): String {
    return protectedProperty!!
  }
}

fun box(): String {
  return A().test()
}
