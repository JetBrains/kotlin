fun test(a: Int) {
    run f@{
      if (a > 0) return@f
      else return@f <!RETURN_TYPE_MISMATCH!>1<!>
    }
}

fun <T> run(f: () -> T): T { return f() }