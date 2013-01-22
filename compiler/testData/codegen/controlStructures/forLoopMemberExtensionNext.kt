class It {
    var hasNext = true
    fun hasNext() = if (hasNext) {hasNext = false; true} else false
}

class C {
    fun iterator(): It = It()
}

class X {
    fun It.next() = 5

    fun test() {
        for (i in C()) {
            foo(i)
        }
    }

}

fun foo(x: Int) {}

fun main(args: Array<String>) {
    X().test()
}
