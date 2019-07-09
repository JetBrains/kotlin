fun some(a: Int, b: Int) {
    ~run {
        val i = 12
        val j = 13
        if (a > 50) {
            if (b > 100) {
                i + j
            } else {
                return@some
            }
        } else {
            <info descr="null">return@run</info>
        }
    }
}

inline fun <T> run(a: () -> Unit) {
}