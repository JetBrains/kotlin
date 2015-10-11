class Delegate {
    fun getValue(t: Any?, p: PropertyMetadata): Int = 1
}

interface A {
    val prop: Int
}

class AImpl: A  {
  override val prop: Int by Delegate()
}

fun box(): String {
  return if(AImpl().prop == 1) "OK" else "fail"
}
