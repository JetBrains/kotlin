package first

import java.util.ArrayList

fun firstFun() {
  val a = ArrayList<Int>()
  a.toLinke<caret>
}

// RUNTIME: 1
// TIME: 1
// EXIST: toLinkedList~() defined in kotlin
// EXIST: toLinkedList~() defined in kotlin.nullable
// NUMBER: 2