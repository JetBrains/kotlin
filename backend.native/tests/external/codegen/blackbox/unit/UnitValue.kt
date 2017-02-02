// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

fun foo() {}

fun box(): String {
  return if (foo() == Unit) "OK" else "Fail"
}
