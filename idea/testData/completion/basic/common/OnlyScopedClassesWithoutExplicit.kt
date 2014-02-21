package first

fun firstFun() {
  val a = In<caret>
}

// INVOCATION_COUNT: 0
// EXIST: { lookupString:"Int", tailText:" (kotlin)" }
// ABSENT: { lookupString:"IntRef", tailText:" (kotlin.internal.Ref)" }
