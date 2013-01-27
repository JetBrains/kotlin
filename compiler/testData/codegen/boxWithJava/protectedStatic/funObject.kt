object A: funObject() {
  fun test(): String {
    return funObject.protectedFun()!!
  }
}

fun box(): String {
  return A.test()
}
