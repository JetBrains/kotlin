// TARGET_BACKEND: JS

fun box(): String {
    val foo = js("{ bar: function(x, y) { return y(x) } }")
    val bar = js("{ baz: 'OK' }")
    return foo.bar(bar) { x -> x.baz }
}