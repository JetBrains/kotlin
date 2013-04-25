class Delegate {
    fun get(t: Any?, p: PropertyMetadata): Int = 1
}

trait A {
    val prop: Int
}

class AImpl: A  {
  override val prop: Int by Delegate()
}

fun box(): String {
  return if(AImpl().prop == 1) "OK" else "fail"
}
