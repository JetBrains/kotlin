import java.*
import java.util.*
import utils.*

import java.io.PrintStream
import java.lang.Comparable as Com

fun <T> checkSubtype(t: T) = t

val l : MutableList<in Int> = ArrayList<Int>()

fun test(l : List<Int>) {
  val x : <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: java.List">java.List</error>
  val y : List<Int>
  val b : java.lang.Object
  val a : java.util.List<Int>
  val z : <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: java.utils.List<R|kotlin/Int|>">java.utils.List<Int></error>

  val f : java.io.File? = null

  Collections.<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: emptyList">emptyList</error>
  Collections.<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: emptyList">emptyList</error><Int>
  Collections.emptyList<Int>()
  Collections.emptyList()

  checkSubtype<Set<Int>?>(Collections.singleton<Int>(1))
  Collections.<error descr="[INAPPLICABLE_CANDIDATE] Inapplicable candidate(s): java/util/Collections.singleton">singleton</error><Int>(1.0)

  List<Int>


  val o = "sdf" as Object

  try {
    // ...
  }
  catch(e: Throwable) {
    System.out.println(e.message)
  }

  PrintStream("sdf")

  val c : Com<Int>? = null

  checkSubtype<java.lang.Comparable<Int>?>(c)

//  Collections.sort<Integer>(ArrayList<Integer>())
}
