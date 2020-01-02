// !WITH_NEW_INFERENCE
fun <T: Any> fooT22() : T? {
  return null
}

fun foo1() {
    fooT22()
}

val n : Nothing = null.sure()

fun <T : Any> T?.sure() : T = this!!
