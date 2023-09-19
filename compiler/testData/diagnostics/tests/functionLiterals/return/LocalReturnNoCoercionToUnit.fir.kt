fun test(a: Int) {
    run f@{
      if (a > 0) <!RETURN_TYPE_MISMATCH!>return@f<!>
      else return@f 1
    }
}
