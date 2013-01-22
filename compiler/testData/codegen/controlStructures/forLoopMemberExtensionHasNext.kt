class It {
    fun next() = 5
}

class C {
    fun iterator(): It = It()
}

class X {
    var hasNext = true
    fun It.hasNext() = if (hasNext) {hasNext = false; true} else false

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
