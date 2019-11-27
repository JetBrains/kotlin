// IGNORE_BACKEND_FIR: JVM_IR
// EA-38323 - Illegal field modifiers in class: classObject field in C must be static and final 

interface C {
  companion object {
    public val FOO: String = "OK"
  }
}

fun box(): String {
  return C.FOO
}

