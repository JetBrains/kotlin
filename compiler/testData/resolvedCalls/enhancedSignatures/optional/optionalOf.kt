import java.util.*

fun use() {
    val x: String? = "x"
    Optional.<caret>of(x)

    Optional.<caret>of(x!!)
    Optional.<caret>ofNullable(x)
}
