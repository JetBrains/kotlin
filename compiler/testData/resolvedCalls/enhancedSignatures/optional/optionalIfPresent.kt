import java.util.*

fun use(v: Optional<String>) {
    v.<caret>ifPresent { value ->  }
}

fun use2(v: Optional<String?>) {
    v.<caret>ifPresent { value ->  }
}
