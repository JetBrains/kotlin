class A {
  var x
    get() = 1
    s<caret>et(value) {
      doSmth(value)
    }
}

fun doSmth(i: String) = 4
