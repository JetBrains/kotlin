fun test(a: Int) {
    run f@{ <!DEPRECATED_LAMBDA_SYNTAX!>(): Int<!> ->
      if (a > 0) return@f <!TYPE_MISMATCH!>""<!>
      return@f 1
    }

    run { <!DEPRECATED_LAMBDA_SYNTAX!>(): Int<!> -> <!TYPE_MISMATCH!>""<!> }
    run { <!DEPRECATED_LAMBDA_SYNTAX!>(): Int<!> -> 1 }
}

fun run<T>(f: () -> T): T { return f() }