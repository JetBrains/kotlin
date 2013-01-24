object A: protectedStaticFunObject() {
  fun test(): String {
    return protectedStaticFunObject.protectedFun()!!
  }
}

fun box(): String {
  return A.test()
}
