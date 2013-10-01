object NamedObject

fun test() {
  val a : Named<caret>
}

// INVOCATION_COUNT: 2
// ABSENT: NamedObject
