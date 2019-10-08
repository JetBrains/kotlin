fun <T> materialize(): T = throw Exception()

interface A

fun takeA(a: A) {}

fun test() {
    takeA(if(true) materialize() else materialize())
}