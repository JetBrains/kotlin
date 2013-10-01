package first

fun firstFun() {
  val a = In<caret>
}

// INVOCATION_COUNT: 0
// EXIST: Int~(jet)
// ABSENT: Int~(jet.runtime.SharedVar)