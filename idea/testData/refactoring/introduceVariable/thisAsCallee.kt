fun (String.() -> String).foo(x : String) {
    x.<selection>this</selection>()
    x.this@foo()
    val t = this
    val tt = this@foo
}