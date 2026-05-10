// LANGUAGE: +CompanionBlocksAndExtensions

class Foo

fun Foo.regular1() {

}

companion val Foo.static1: Int get() = 1

val Foo.regular2: String get() = "r2"
fun Foo.regular3() {}

companion val Foo.static2: Long get() = 2L
companion fun Foo.static3() {}
