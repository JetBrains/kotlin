fun bar(vararg x: Int) {
    x.forEach {
        println(it)
    }
    println("size: ${x.size}")
}

inline fun foo() = bar(17, 19, 23, *intArrayOf(29, 31))

