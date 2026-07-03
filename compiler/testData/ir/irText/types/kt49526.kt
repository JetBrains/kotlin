// WITH_STDLIB
// SKIP_KT_DUMP
// DUMP_IR_DIFFERENCE: JVM
//   On non-JVM backendsm, the inferred type of `+` expression is `Comparable<Nothing>` as the intersection of Comparable<Char> and Comparable<String>:
//   On JVM backend, kotlin.Char is mapped to the Java primitive char boxed as java.lang.Character, and this mapping changes its type hierarchy.
//     So, the inferred type of `+` is Any as as the closest common supertype for Char and String.


fun test(): Boolean {
    val ref = (listOf('a') + "-")::contains
    return ref('a')
}
