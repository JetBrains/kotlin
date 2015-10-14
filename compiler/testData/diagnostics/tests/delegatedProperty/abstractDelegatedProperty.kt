// !DIAGNOSTICS: -UNUSED_PARAMETER

abstract class A {
    abstract val a: Int <!ABSTRACT_DELEGATED_PROPERTY!>by Delegate()<!>
}

class Delegate {
  operator fun getValue(t: Any?, p: PropertyMetadata): Int {
    return 1
  }
}