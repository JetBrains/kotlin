import java.util.*

fun use(v: Optional<String>) {
    v.<caret>get()
}

fun use2(v: Optional<String?>) {
    v.<caret>get()
}
