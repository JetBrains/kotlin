fun foo() {}

fun box(): String {
  return if (foo() == Unit.VALUE) "OK" else "Fail"
}
