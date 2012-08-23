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
            System.out.println(i)
        }
    }

}

fun main(args: Array<String>) {
    X().test()
}