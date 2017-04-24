fun test() {
    <info descr="null">~foo</info>@ for (n in 1..10) {
        if (n == 5) continue@<info descr="null">foo</info>
        if (n > 8) break@<info descr="null">foo</info>
    }
}