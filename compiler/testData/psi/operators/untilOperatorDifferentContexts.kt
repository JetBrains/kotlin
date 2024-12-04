fun main() {
    0..<n
}

fun main(val x: Int = 0..<n) {}

@Suppress(0 ..< n)
fun main() {}

fun main() {
    when {
        0 ..< n -> true
        n..<n+1 -> true
        n+1..<n+2 -> true
        n * 2 ..< n * 3 -> true
    }
}

fun main() {
    if (0..<n..<n) {} else if (0..n..<n..n) {} else if (0 ..< n .. n ..< n ..< n) {} else { ((0+1..<n-1..1+n)-1..<3*n) ..< n }
}
