fun foo(<!UNUSED_PARAMETER!>a<!> : Any) {}

fun test() {
  foo(object {});
}
