// WITH_RUNTIME

fun foo() {
    val list = listOf(MyClass(1, 2, 3), MyClass(2, 3, 4))
    list.forEach { klass<caret> ->
        val a = klass.a
        val b = klass.b
    }
}

data class MyClass(val a: Int, val b: Int, val c: Int)