// ISSUE: KT-67764

class CustomList<in T>{}

fun m(l: Any) {
    val asList = l is CustomList<out String>
}
