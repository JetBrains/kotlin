/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-152
 * PRIMARY LINKS: expressions, when-expression -> paragraph 2 -> sentence 5
 * expressions, when-expression -> paragraph 5 -> sentence 1
 * expressions, when-expression -> paragraph 6 -> sentence 1
 * expressions, when-expression -> paragraph 6 -> sentence 3
 * expressions, when-expression -> paragraph 6 -> sentence 5
 * expressions, when-expression -> paragraph 6 -> sentence 9
 * expressions, when-expression -> paragraph 6 -> sentence 10
 * expressions, when-expression -> paragraph 6 -> sentence 11
 */

fun Int.foo() : Boolean = true

fun foo() : Int {
    val s = ""
    val x = 1
    when (x) {
      is <!INCOMPATIBLE_TYPES!>String<!> -> 1
      <!USELESS_IS_CHECK!>!is Int<!> -> 1
      <!USELESS_IS_CHECK!>is Any<!USELESS_NULLABLE_CHECK!>?<!><!> -> 1
      <!USELESS_IS_CHECK!>is Any<!> -> 1
      <!INCOMPATIBLE_TYPES!>s<!> -> 1
      1 -> 1
      1 + <!UNRESOLVED_REFERENCE!>a<!> -> 1
      in 1..<!UNRESOLVED_REFERENCE!>a<!> -> 1
      !in 1..<!UNRESOLVED_REFERENCE!>a<!> -> 1
      else -> 1
    }

    return 0
}

val _type_test : Int = foo() // this is needed to ensure the inferred return type of foo()

fun test() {
  val x = 1;
  val s = "";

  when (x) {
    <!INCOMPATIBLE_TYPES!>s<!> -> 1
    <!DUPLICATE_LABEL_IN_WHEN, INCOMPATIBLE_TYPES!>""<!> -> 1
    x -> 1
    <!DUPLICATE_LABEL_IN_WHEN!>1<!> -> 1
  }

  val z = 1

  when (z) {
    <!ELSE_MISPLACED_IN_WHEN!>else<!> -> 1
    <!UNREACHABLE_CODE!>1 -> 2<!>
  }

  when (z) {
    else -> 1
  }
}
