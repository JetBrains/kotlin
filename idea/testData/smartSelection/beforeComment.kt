class A {
    val list = listOf(1, 2, 3)
}

fun foo(a: A) {
    for (x in a.list<caret>/* comment */) {
        println(x)
    }
}

/*
a.list
*/