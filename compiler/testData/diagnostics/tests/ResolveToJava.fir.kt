// !WITH_NEW_INFERENCE
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

  Collections.<!UNRESOLVED_REFERENCE!>emptyList<!>
  Collections.<!UNRESOLVED_REFERENCE!>emptyList<!><Int>
  Collections.emptyList<Int>()
  Collections.emptyList()

  checkSubtype<Set<Int>?>(Collections.singleton<Int>(1))
  Collections.singleton<Int>(<!ARGUMENT_TYPE_MISMATCH!>1.0<!>)

  List<Int>


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
