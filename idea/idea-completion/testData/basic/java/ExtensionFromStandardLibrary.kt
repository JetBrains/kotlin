package first

import java.util.HashMap

fun firstFun() {
  val a = HashMap<Int, String>()
  a.toLinke<caret>
}

// INVOCATION_COUNT: 1
// EXIST: { lookupString:"toLinkedMap", itemText:"toLinkedMap", tailText:"() for Map<K, V> in kotlin.collections" }
// NOTHING_ELSE
