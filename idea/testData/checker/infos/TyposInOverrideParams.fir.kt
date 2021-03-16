abstract class Base {
  abstract fun foo(oher: Int)

  abstract val smalVal: Int
  abstract fun smalFun()
}

class Other : Base() {
  override fun foo(oher: Int) {
  }

  override val smalVal: Int get() = 1
  override fun smalFun() {}
}
