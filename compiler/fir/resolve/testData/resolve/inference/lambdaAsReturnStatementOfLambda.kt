// ISSUE: KT-37070

class A

fun test(a: A) {

    val lambda = a.let {
        { it }
    }

    val alsoA = lambda()
    takeA(alsoA)
}

fun takeA(a: A) {}
