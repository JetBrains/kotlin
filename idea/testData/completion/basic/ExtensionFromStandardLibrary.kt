package first

import java.util.ArrayList

fun firstFun() {
  val a = ArrayList<Int>()
  a.toLinke<caret>
}

// RUNTIME: 1
// TIME: 1
// EXIST: toLinkedList@toLinkedList()~for java.lang.Iterable<T> in kotlin
// EXIST: toLinkedList@toLinkedList()~for T? in kotlin.nullable
// NUMBER: 2