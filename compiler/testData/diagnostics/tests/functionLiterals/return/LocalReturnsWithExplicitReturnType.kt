fun test(a: Int) {
    run<Int>f@{
      if (a > 0) return@f <!TYPE_MISMATCH!>""<!>
      return@f 1
    }

    run<Int>{ <!TYPE_MISMATCH!>""<!> }
    run<Int>{ 1 }
}

fun <T> run(f: () -> T): T { return f() }