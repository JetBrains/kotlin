class It {
}

class C {
}

class X {
    var hasNext = true
    operator fun It.hasNext() = if (hasNext) {hasNext = false; true} else false
    operator fun It.next() = 5
    operator fun C.iterator(): It = It()

    fun test() {
        for (i in C()) {
            foo(i)
        }
    }

}

fun foo(x: Int) {}

fun box(): String {
    X().test()
    return "OK"
}
