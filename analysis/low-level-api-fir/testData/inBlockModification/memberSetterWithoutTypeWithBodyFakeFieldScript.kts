class A {
  var x
    get() = 1
    set(value) {
      val field = value
      <expr>field</expr>
    }
}
