// EA-38323 - Illegal field modifiers in class: classObject field in C must be static and final 

trait C {
  default object {
    public val FOO: String = "OK"
  }
}

fun box(): String {
  return C.FOO
}

