class MyRunnable() {}

deprecated("Use A instead") fun MyRunnable.invoke() {
}

fun test() {
  val m = MyRunnable()
  <warning descr="'fun invoke()' is deprecated. Use A instead">m()</warning>
}

// NO_CHECK_INFOS
// NO_CHECK_WEAK_WARNINGS