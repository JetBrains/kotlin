// !WITH_NEW_INFERENCE
class C<T>() {
  fun foo() : T {}
}

fun foo(c: C<Int>) {}
fun <T> bar() : C<T> {}

fun main() {
  val a : C<Int> = C();
  val x : C<in String> = C()
  val y : C<out String> = C()
  val z : C<*> = C()

  val ba : C<Int> = bar();
  val bx : C<in String> = bar()
  val by : C<out String> = bar()
  val bz : C<*> = bar()
}