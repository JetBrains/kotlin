enum class Test(a : Int) {
  A : Test(0)
  B(x : Int) : Test(x)
  C : Test(0) {}
}
