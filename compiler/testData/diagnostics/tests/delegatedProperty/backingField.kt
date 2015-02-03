// !DIAGNOSTICS: -UNUSED_PARAMETER

class B {
    val a: Int by Delegate()

    fun foo() = <!NO_BACKING_FIELD_CUSTOM_ACCESSORS!>$a<!>
}

class Delegate {
  fun get(t: Any?, p: PropertyMetadata): Int {
    return 1
  }
}

