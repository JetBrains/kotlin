fun foo(): Int {
    @annotation class Ann
    @Ann val x = 1
    return x
}