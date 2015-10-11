// !DIAGNOSTICS: -UNUSED_PARAMETER

val Int.a by Delegate(<!NO_THIS!>this<!>)

class A {
  val Int.a by Delegate(<!TYPE_MISMATCH!>this<!>)
}

class Delegate(i: Int) {
  fun getValue(t: Any?, p: PropertyMetadata): Int {
    return 1
  }
}