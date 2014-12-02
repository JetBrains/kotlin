package first

import java.util.ArrayList

fun firstFun() {
  val a = ArrayList<Int>()
  a.toLinke<caret>
}

// INVOCATION_COUNT: 1
// EXIST: { lookupString:"toLinkedList", itemText:"toLinkedList", tailText:"() for Iterable<Int!> in kotlin" }
// NUMBER: 1
