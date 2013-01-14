class MyRunnable() {}

deprecated("Use A instead") fun MyRunnable.invoke() {
}

fun test() {
  val m = MyRunnable()
  <info descr="'fun invoke()' is deprecated. Use A instead"><info>m</info>()</info>
}