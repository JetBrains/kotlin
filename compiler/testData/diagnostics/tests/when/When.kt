// !WITH_NEW_INFERENCE
/*
 * RELEVANT SPEC SENTENCES (spec version: 0.1-152, test type: neg):
 *  - expressions, when-expression -> paragraph 2 -> sentence 5
 *  - expressions, when-expression -> paragraph 5 -> sentence 1
 *  - expressions, when-expression -> paragraph 6 -> sentence 1
 *  - expressions, when-expression -> paragraph 6 -> sentence 3
 *  - expressions, when-expression -> paragraph 6 -> sentence 5
 *  - expressions, when-expression -> paragraph 6 -> sentence 9
 *  - expressions, when-expression -> paragraph 6 -> sentence 10
 *  - expressions, when-expression -> paragraph 6 -> sentence 11
 */

fun Int.foo() : Boolean = true

fun foo() : Int {
    val s = ""
    val x = 1
    when (x) {
      is <!INCOMPATIBLE_TYPES!>String<!> -> <!UNUSED_EXPRESSION!>1<!>
      <!USELESS_IS_CHECK!>!is Int<!> -> <!UNUSED_EXPRESSION!>1<!>
      <!USELESS_IS_CHECK!>is Any<!USELESS_NULLABLE_CHECK!>?<!><!> -> <!UNUSED_EXPRESSION!>1<!>
      <!USELESS_IS_CHECK!>is Any<!> -> <!UNUSED_EXPRESSION!>1<!>
      <!INCOMPATIBLE_TYPES!>s<!> -> <!UNUSED_EXPRESSION!>1<!>
      1 -> <!UNUSED_EXPRESSION!>1<!>
      1 + <!UNRESOLVED_REFERENCE!>a<!> -> <!UNUSED_EXPRESSION!>1<!>
      in 1..<!UNRESOLVED_REFERENCE!>a<!> -> <!UNUSED_EXPRESSION!>1<!>
      !in 1..<!UNRESOLVED_REFERENCE!>a<!> -> <!UNUSED_EXPRESSION!>1<!>
      else -> <!UNUSED_EXPRESSION!>1<!>
    }

    return 0
}

val _type_test : Int = foo() // this is needed to ensure the inferred return type of foo()

fun test() {
  val x = 1;
  val s = "";

  when (x) {
    <!INCOMPATIBLE_TYPES!>s<!> -> <!UNUSED_EXPRESSION!>1<!>
    <!DUPLICATE_LABEL_IN_WHEN, INCOMPATIBLE_TYPES!>""<!> -> <!UNUSED_EXPRESSION!>1<!>
    x -> <!UNUSED_EXPRESSION!>1<!>
    1 -> <!UNUSED_EXPRESSION!>1<!>
  }

  val z = 1

  when (z) {
    <!ELSE_MISPLACED_IN_WHEN!>else<!> -> <!UNUSED_EXPRESSION!>1<!>
    <!UNREACHABLE_CODE!>1 -> 2<!>
  }

  when (<!UNUSED_EXPRESSION!>z<!>) {
    else -> <!UNUSED_EXPRESSION!>1<!>
  }
}