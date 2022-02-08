// FIR_IDENTICAL
class C<T>() {
  fun foo() : T {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
}

fun foo(c: C<Int>) {}
fun <T> bar() : C<T> {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

fun main() {
  val a : C<Int> = C();
  val x : C<in String> = C()
  val y : C<out String> = C()
  val z : C<*> = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>C<!>()

  val ba : C<Int> = bar();
  val bx : C<in String> = bar()
  val by : C<out String> = bar()
  val bz : C<*> = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar<!>()
}
