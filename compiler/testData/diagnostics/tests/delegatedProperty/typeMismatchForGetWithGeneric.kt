// !DIAGNOSTICS: -UNUSED_PARAMETER
class A

class B {
  val b: Int by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>Delegate<A>()<!>
}

val bTopLevel: Int by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>Delegate<A>()<!>

class C {
  val c: Int by Delegate<C>()
}

val cTopLevel: Int by Delegate<Nothing?>()

class Delegate<T> {
  fun get(t: T, p: PropertyMetadata): Int {
    return 1
  }
}