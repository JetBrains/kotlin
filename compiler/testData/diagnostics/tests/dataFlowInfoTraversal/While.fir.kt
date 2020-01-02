fun bar(x: Int): Int = x + 1

fun foo() {
    val x: Int? = null
    while (x == null) {
        bar(x)
    }
    bar(x)
    
    val y: Int? = null
    while (y != null) {
        bar(y)
    }
    bar(y)
    
    val z: Int? = null
    while (z == null) {
        bar(z)
        break
    }
    bar(z)
}
