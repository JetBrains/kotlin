fun foo(): Int {
    <expr>for(i in 1..10) {
        println(i)
        if (i == 5) {
            return i
        }
    }</expr>

    return 0
}
