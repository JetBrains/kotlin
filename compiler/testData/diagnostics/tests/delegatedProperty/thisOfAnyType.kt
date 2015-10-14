// !DIAGNOSTICS: -UNUSED_PARAMETER

class A {
  var a: Int by Delegate()
}

var aTopLevel: Int by Delegate()

class Delegate {
  operator fun getValue(t: Any?, p: PropertyMetadata): Int {
    return 1
  }
  operator fun setValue(t: Any?, p: PropertyMetadata, a: Int) {}
}