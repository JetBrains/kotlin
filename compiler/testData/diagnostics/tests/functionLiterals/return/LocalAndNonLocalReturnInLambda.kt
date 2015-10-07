// !CHECK_TYPE

fun test2(a: Int) {
    val x = run f@{
      if (a > 0) <!RETURN_NOT_ALLOWED!>return<!>
      return@f 1
    }
    checkSubtype<Int>(x)
}

fun <T> run(f: () -> T): T { return f() }