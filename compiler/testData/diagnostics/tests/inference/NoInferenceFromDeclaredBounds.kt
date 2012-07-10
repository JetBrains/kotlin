fun fooT22<T>() : T? {
  return null
}

fun foo1() {
    <!TYPE_INFERENCE_FAILED!>fooT22()<!>
}

val n : Nothing = null.sure()
