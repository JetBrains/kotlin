fun test() {
    bar@ for (n in 1..10) {
        if (n == 5) continue@bar
        if (n > 8) break@bar
    }
}