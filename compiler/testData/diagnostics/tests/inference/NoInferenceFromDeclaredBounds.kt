// !WITH_NEW_INFERENCE
fun <T: Any> fooT22() : T? {
  return null
}

fun foo1() {
    <!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>fooT22<!>()
}

val n : Nothing = null.<!OI;IMPLICIT_NOTHING_AS_TYPE_PARAMETER!>sure<!>()

fun <T : Any> T?.sure() : T = this!!
