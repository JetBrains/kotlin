// WITH_STDLIB
// ISSUE: KT-66793

// FILE: GetArray.java
public class J {
  public static String[] getArray() {
    String[] res = {"a", "b"};
    return res;
  }
}

// FILE: main.kt
fun call(vararg s: String) {}

fun getArray(): Array<String>? = null

fun callCall() {
  call(s = getArray() ?: error("error"))
  call(s = J.getArray() ?: arrayOf("a"))
  call(s = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_ERROR!>J.getArray() ?: error("error")<!>)
}
