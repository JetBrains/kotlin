// !WITH_NEW_INFERENCE
fun test(a: Int) {
    run f@{
      if (a > 0) return@f
      else return@f <!OI;RETURN_TYPE_MISMATCH!>1<!>
    }
}
