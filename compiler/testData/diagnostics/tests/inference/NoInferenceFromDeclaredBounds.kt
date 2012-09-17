fun fooT22<T>() : T? {
  return null
}

fun foo1() {
    <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>fooT22<!>()
}

val n : Nothing = null!!

fun <T : Any> T?.sure() : T = this!!
