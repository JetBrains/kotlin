package first

import java.util.ArrayList

fun firstFun() {
  val a = ArrayList<Int>()
  a.toLinke<caret>
}

// INVOCATION_COUNT: 1
// EXIST: toLinkedList@toLinkedList()~for jet.Iterable<T> in kotlin
// NUMBER: 1
