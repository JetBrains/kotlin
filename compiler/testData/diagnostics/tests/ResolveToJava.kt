// FILE: f.kt

import java.*
import java.util.*
import <!UNRESOLVED_REFERENCE!>utils<!>.*

import java.io.PrintStream
import java.lang.Comparable as Com

val l : List<in Int> = ArrayList<Int>()

fun test(<!UNUSED_PARAMETER!>l<!> : java.util.List<Int>) {
  val <!UNUSED_VARIABLE!>x<!> : java.<!UNRESOLVED_REFERENCE!>List<!>
  val <!UNUSED_VARIABLE!>y<!> : java.util.List<Int>
  val <!UNUSED_VARIABLE!>b<!> : java.lang.Object
  val <!UNUSED_VARIABLE!>a<!> : util.List<Int>
  val <!UNUSED_VARIABLE!>z<!> : java.<!UNRESOLVED_REFERENCE!>utils<!>.List<Int>

  val <!UNUSED_VARIABLE!>f<!> : java.io.File? = null

  Collections.<!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER, FUNCTION_CALL_EXPECTED!>emptyList<!>
  Collections.<!FUNCTION_CALL_EXPECTED!>emptyList<Int><!>
  Collections.emptyList<Int>()
  Collections.<!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>emptyList<!>()

  Collections.singleton<Int>(1) : Set<Int>?
  Collections.singleton<Int>(<!ERROR_COMPILE_TIME_VALUE!>1.0<!>)

  <!UNRESOLVED_REFERENCE!>List<!><Int>


  val <!UNUSED_VARIABLE!>o<!> = "sdf" <!CAST_NEVER_SUCCEEDS!>as<!> Object

  try {
    // ...
  }
  catch(e: Exception) {
    System.out.println(e.getMessage())
  }

  PrintStream("sdf")

  val c : Com<Int>? = null

  c : java.lang.Comparable<Int>?

//  Collections.sort<Integer>(ArrayList<Integer>())
  xxx.<!UNRESOLVED_REFERENCE!>Class<!>()
}


// FILE: f.kt
package xxx
  import java.lang.Class;