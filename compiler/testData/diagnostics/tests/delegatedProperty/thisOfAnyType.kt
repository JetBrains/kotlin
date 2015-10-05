// !DIAGNOSTICS: -UNUSED_PARAMETER

class A {
  var a: Int by Delegate()
}

var aTopLevel: Int by Delegate()

class Delegate {
  fun getValue(t: Any?, p: PropertyMetadata): Int {
    return 1
  }
  fun setValue(t: Any?, p: PropertyMetadata, a: Int) {}
}