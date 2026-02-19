class Foo {
    val (a, b) = transformation {
        val localProperty = 1
        MyPair(<expr>localProperty</expr>, "str")
    }
}

fun <T> transformation(body: () -> T): T = body()

data class MyPair(val l: Int, r: String)