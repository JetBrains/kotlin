class MyRunnable() {}

Deprecated fun MyRunnable.invoke() {
}

fun test() {
  val m = MyRunnable()
  <info descr="'fun invoke()' is deprecated">m()</info>
}