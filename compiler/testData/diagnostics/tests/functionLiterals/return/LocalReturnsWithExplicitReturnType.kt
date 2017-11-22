// !WITH_NEW_INFERENCE
fun test(a: Int) {
    run<Int>f@{
      if (a > 0) return@f <!NI;TYPE_MISMATCH, TYPE_MISMATCH!>""<!>
      return@f 1
    }

    run<Int>{ <!NI;TYPE_MISMATCH, TYPE_MISMATCH!>""<!> }
    run<Int>{ 1 }
}
