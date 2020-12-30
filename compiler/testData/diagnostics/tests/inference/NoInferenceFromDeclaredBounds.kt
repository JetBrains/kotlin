// !WITH_NEW_INFERENCE
fun <T: Any> fooT22() : T? {
  return null
}

fun foo1() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER{NI}, TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER{OI}!>fooT22<!>()
}

val n : Nothing = null.sure()

fun <T : Any> T?.sure() : T = this!!
