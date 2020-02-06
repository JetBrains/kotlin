// WITH_RUNTIME
data class Column(val name: String)

fun test(columns: List<Column>) {
    val sql = <caret>"""
        SELECT ${columns.joinToString(", ")} FROM foo
    """.trimIndent()
}