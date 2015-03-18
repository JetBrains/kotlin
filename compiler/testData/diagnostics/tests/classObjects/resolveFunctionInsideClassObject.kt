package test

class Test {
  fun test(): Int = 12

  companion object {
    val a = <!INACCESSIBLE_OUTER_CLASS_EXPRESSION!>test()<!> // Check if resolver will be able to infer type of a variable
  }
}