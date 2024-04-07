// FIR_IDENTICAL
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
  call(s = J.getArray() ?: error("error"))
}
