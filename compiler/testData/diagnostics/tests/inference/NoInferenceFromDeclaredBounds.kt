// !WITH_NEW_INFERENCE
fun <T: Any> fooT22() : T? {
  return null
}

fun foo1() {
    <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>fooT22<!>()
}

val n : Nothing = null.sure()

fun <T : Any> T?.sure() : T = this!!
