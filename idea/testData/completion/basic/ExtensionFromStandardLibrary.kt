package first

import java.util.ArrayList

fun firstFun() {
  val a = ArrayList<Int>()
  a.toLinke<caret>
}

// RUNTIME: 1
// TIME: 1
// EXIST: toLinkedList~() in kotlin
// EXIST: toLinkedList~() in kotlin.nullable
// NUMBER: 2