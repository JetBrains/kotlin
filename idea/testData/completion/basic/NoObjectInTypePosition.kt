object NamedObject

fun test() {
  val a : Named<caret>
}

// TIMES: 2
// ABSENT: NamedObject
