// ISSUE: KT-63923
// RENDER_DIAGNOSTICS_FULL_TEXT

val number: Number = 1f

class Box<T>(var item: T)

val inNumberBox: Box<in Number> = Box<Any?>("3")

fun <T : Number> numberBoxHandler(box: Box<T>, t: T) {}
fun <T : Number> outNumberBoxHandler(box: Box<out T>, t: T) {}

fun main() {
    numberBoxHandler(<!TYPE_MISMATCH!>inNumberBox<!>, <!TYPE_MISMATCH!>number<!>)
    outNumberBoxHandler(<!TYPE_MISMATCH!>inNumberBox<!>, <!TYPE_MISMATCH!>number<!>)
}
