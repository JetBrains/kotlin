// !CHECK_TYPE
// SKIP_JAVAC
// FULL_JDK

// FILE: a.kt

import java.*
import java.util.*
import utils.*

import java.io.PrintStream
import java.lang.Comparable as Com

val l : MutableList<in Int> = ArrayList<Int>()

fun test(l : java.util.List<Int>) {
  val x : <!UNRESOLVED_REFERENCE!>java.List<!>
  val y : java.util.List<Int>
  val b : java.lang.Object
  val z : <!UNRESOLVED_REFERENCE!>java.utils.List<Int><!>

  val f : java.io.File? = null

  Collections.<!FUNCTION_CALL_EXPECTED!>emptyList<!>
  Collections.<!FUNCTION_CALL_EXPECTED!>emptyList<!><<!CANNOT_INFER_PARAMETER_TYPE!>Int<!>>
  Collections.emptyList<Int>()
  Collections.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>emptyList<!>()

  checkSubtype<Set<Int>?>(Collections.singleton<Int>(1))
  Collections.singleton<Int>(<!ARGUMENT_TYPE_MISMATCH!>1.0<!>)

  <!NO_COMPANION_OBJECT!>List<!><Int>


  val o = "sdf" as Object

  try {
    // ...
  }
  catch(e: Exception) {
    System.out.println(e.message)
  }

  PrintStream("sdf")

  val c : Com<Int>? = null

  checkSubtype<java.lang.Comparable<Int>?>(c)

//  Collections.sort<Integer>(ArrayList<Integer>())
  xxx.<!UNRESOLVED_REFERENCE!>Class<!>()
}


// FILE: b.kt
package xxx
  import java.lang.Class;
