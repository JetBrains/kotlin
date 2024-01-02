// ISSUE: KT-64640
// WITH_STDLIB

fun bar(x: List<String>) {
    x.addFirst("")
    x.addLast("")
    x.removeFirst()
    x.removeLast()
}