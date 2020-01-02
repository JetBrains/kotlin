// !WITH_NEW_INFERENCE
fun test(a: Int) {
    run<Int>f@{
      if (a > 0) return@f ""
      return@f 1
    }

    run<Int>{ "" }
    run<Int>{ 1 }
}
