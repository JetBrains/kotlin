fun test(a: Int) {
    run<Int>f@{
      if (a > 0) return@f <!ARGUMENT_TYPE_MISMATCH, RETURN_TYPE_MISMATCH!>""<!>
      return@f 1
    }

    run<Int>{ <!ARGUMENT_TYPE_MISMATCH!>""<!> }
    run<Int>{ 1 }
}
