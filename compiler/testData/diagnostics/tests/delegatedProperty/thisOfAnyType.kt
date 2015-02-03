// !DIAGNOSTICS: -UNUSED_PARAMETER

class A {
  var a: Int by Delegate()
}

var aTopLevel: Int by Delegate()

class Delegate {
  fun get(t: Any?, p: PropertyMetadata): Int {
    return 1
  }
  fun set(t: Any?, p: PropertyMetadata, a: Int) {}
}