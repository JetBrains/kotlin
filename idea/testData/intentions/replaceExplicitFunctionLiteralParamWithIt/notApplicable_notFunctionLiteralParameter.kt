// IS_APPLICABLE: false

val foo = { x: Int ->
    class Inner() {
      fun temp(<caret>y: Int) : Int { return x + y }
    }
    Inner()
}
