// RUN_PIPELINE_TILL: FRONTEND
fun test(a: Int) {
    run<Int>f@{
      if (a > 0) return@f <!RETURN_TYPE_MISMATCH!>""<!>
      return@f 1
    }

    run<Int>{ <!RETURN_TYPE_MISMATCH!>""<!> }
    run<Int>{ 1 }
}
