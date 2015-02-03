// !DIAGNOSTICS: -UNUSED_PARAMETER

trait A {
    val prop: Int
}

class AImpl: A  {
    override val prop by Delegate()
}

fun foo() {
    AImpl().prop
}

class Delegate {
  fun get(t: Any?, p: PropertyMetadata): Int {
    return 1
  }
}
