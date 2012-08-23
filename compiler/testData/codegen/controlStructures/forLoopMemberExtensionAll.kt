class It {
}

class C {
}

class X {
    var hasNext = true
    fun It.hasNext() = if (hasNext) {hasNext = false; true} else false
    fun It.next() = 5
    fun C.iterator(): It = It()

    fun test() {
        for (i in C()) {
            System.out.println(i)
        }
    }

}

fun main(args: Array<String>) {
    X().test()
}