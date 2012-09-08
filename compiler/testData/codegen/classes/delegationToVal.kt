trait IActing {
  fun act(): String
}

class CActing(val value: String = "OK"): IActing {
  override fun act(): String = value
}

// final so no need in delegate field
class Test(val acting: CActing = CActing()): IActing by acting {
}

// even if open so we don't need delegate field
open class Test2(open val acting: CActing = CActing()): IActing by acting {
}

// even if open the backing field is final, so we don't need delegate field
class Test3() : Test2() {
  override val acting = CActing("OKOK")
}

fun box(): String {
  val test = Test()
  return test.act()
}
