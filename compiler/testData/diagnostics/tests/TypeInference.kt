class C<T>() {
  fun foo() : T {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
}

fun foo(<!UNUSED_PARAMETER!>c<!>: C<Int>) {}
fun bar<T>() : C<T> {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

fun main(args : Array<String>) {
  val <!UNUSED_VARIABLE!>a<!> : C<Int> = C();
  val <!UNUSED_VARIABLE!>x<!> : C<in String> = C()
  val <!UNUSED_VARIABLE!>y<!> : C<out String> = C()
  val <!UNUSED_VARIABLE!>z<!> : C<*> = <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>C<!>()

  val <!UNUSED_VARIABLE!>ba<!> : C<Int> = bar();
  val <!UNUSED_VARIABLE!>bx<!> : C<in String> = bar()
  val <!UNUSED_VARIABLE!>by<!> : C<out String> = bar()
  val <!UNUSED_VARIABLE!>bz<!> : C<*> = <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar<!>()
}