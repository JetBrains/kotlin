// CHECK_TYPE
// SKIP_JAVAC
// FULL_JDK
// WITH_EXTENDED_CHECKERS

// FILE: a.kt

import java.*
import java.util.*
import <!UNRESOLVED_REFERENCE!>utils<!>.*

import java.io.PrintStream
import <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.lang.Comparable<!> as Com

val l : MutableList<in Int> = ArrayList<Int>()

fun test(l : <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.util.List<Int><!>) {
  val x : java.<!UNRESOLVED_REFERENCE!>List<!>
  val y : <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.util.List<Int><!>
  val b : <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.lang.Object<!>
  val z : java.<!UNRESOLVED_REFERENCE!>utils<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>List<!><Int>

  val f : java.io.File? = null

  Collections.<!FUNCTION_CALL_EXPECTED, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>emptyList<!>
  Collections.<!FUNCTION_CALL_EXPECTED!>emptyList<Int><!>
  Collections.emptyList<Int>()
  Collections.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>emptyList<!>()

  checkSubtype<Set<Int>?>(Collections.singleton<Int>(1))
  Collections.singleton<Int>(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1.0<!>)

  <!RESOLUTION_TO_CLASSIFIER!>List<!><Int>


  val o = "sdf" as <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>Object<!>

  try {
    // ...
  }
  catch(e: Exception) {
    System.out.println(e.message)
  }

  PrintStream("sdf")

  val c : <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>Com<Int><!>? = null

  checkSubtype<<!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.lang.Comparable<Int><!>?>(c)

//  Collections.sort<Integer>(ArrayList<Integer>())
  xxx.<!UNRESOLVED_REFERENCE!>Class<!>()
}


// FILE: b.kt
package xxx
  import java.lang.Class;
