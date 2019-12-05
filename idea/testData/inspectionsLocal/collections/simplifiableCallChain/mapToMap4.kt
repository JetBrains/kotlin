// FIX: Merge call chain to 'associateWith'
// WITH_RUNTIME
fun foo() {}

fun test(list: List<Int>) {
    val map: Map<Int, Int> = list.<caret>map {
        foo()
        foo()
        it to it
    }.toMap()
}