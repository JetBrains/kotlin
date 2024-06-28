// JVM_ABI_K1_K2_DIFF: KT-63828

interface A<T> {
  var zzzValue : T
  fun zzz() : T
}

class Base<T> : A<T?> {
  override var zzzValue : T? = null

  override fun zzz() : T? = zzzValue
}

class X : A<String?> by Base<String?>()

fun box() : String {
  (Base<String?>() as A<String?>).zzz()

  if (X().zzz() != null) {
    return "Fail"
  }

  val x = X()
  x.zzzValue = "aa"
  if (x.zzzValue != "aa") {
    return "Fail 2";
  }
  if (x.zzz() != "aa") {
    return "Fail 3";
  }

  return "OK"
}
