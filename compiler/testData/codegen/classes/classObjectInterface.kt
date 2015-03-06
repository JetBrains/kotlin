class C() {
  fun getInstance(): Runnable = C

  default object: Runnable {
    override fun run(): Unit { }
  }
}

fun foo() = C().getInstance()
