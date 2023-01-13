class Foo<T>(len : Int) {
  constructor(s : String) : this(s.length) {}
}

fun f() {
    <expr>Foo<String></expr>
}
