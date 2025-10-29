// IGNORE_TREE_ACCESS: KT-64899
annotation class Ann(val x: String = "")

data class Foo(val first: String)

val x = Foo("")

@Ann (var first) = x
@Ann(var a = first) = x
@Ann() (var b = first) = x
@Ann("") (var c = first) = x
@Ann(val d = first) = x
@Ann() (val e = first) = x
@Ann("") (val f = first) = x