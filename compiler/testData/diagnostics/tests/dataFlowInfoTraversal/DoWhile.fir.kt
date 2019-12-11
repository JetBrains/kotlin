fun bar(x: Int): Int = x + 1

fun foo() {
    val x: Int? = null

    do {
        bar(x)
    } while (x == null)
    bar(x)
    
    val y: Int? = null
    do {
        bar(y)
    } while (y != null)
    bar(y)
}
