// ISSUE: KT-55703
// DUMP_IR

@JvmRecord
data class Tag(
    val id: String,
) {
    companion object
}

fun box(): String {
    return Tag("OK").id
}
