// PROBLEM: none
// WITH_RUNTIME

fun test(data: HashMap<String, String>) {
    val result = data.<caret>map { "${it.key}: ${it.value}" }.joinToString("\n")
}