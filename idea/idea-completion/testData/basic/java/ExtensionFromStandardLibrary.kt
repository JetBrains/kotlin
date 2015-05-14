package first

import java.util.ArrayList

fun firstFun() {
  val a = ArrayList<Int>()
  a.toLinke<caret>
}

// INVOCATION_COUNT: 1
// EXIST: { lookupString:"toLinkedList", itemText:"toLinkedList", tailText:"() for Iterable<T> in kotlin" }
// NOTHING_ELSE: true
