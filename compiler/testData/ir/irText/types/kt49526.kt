// WITH_STDLIB
// SKIP_KT_DUMP

fun test(): Boolean {
    val ref = (listOf('a') + "-")::contains
    return ref('a')
}
