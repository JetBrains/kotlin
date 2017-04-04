import kotlin.test.*



fun box() {
    val data = "kotlin".toList()
    val sb = StringBuilder()
    data.joinTo(sb, "^", "<", ">")
    assertEquals("<k^o^t^l^i^n>", sb.toString())
}
