package first

fun firstFun() {
  val a = In<caret>
}

// INVOCATION_COUNT: 0
// EXIST: { lookupString:"Int", tailText:" (jet)" }
// ABSENT: { lookupString:"Int", tailText:" (jet.runtime.SharedVar)" }