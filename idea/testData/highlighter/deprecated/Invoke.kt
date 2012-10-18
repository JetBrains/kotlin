class MyRunnable() {}

deprecated("'fun invoke()' is deprecated") fun MyRunnable.invoke() {
}

fun test() {
  val m = MyRunnable()
  <info descr="'fun invoke()' is deprecated">m()</info>
}