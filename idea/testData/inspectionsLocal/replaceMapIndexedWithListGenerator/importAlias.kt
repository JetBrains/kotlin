// WITH_RUNTIME
import kotlin.collections.mapIndexed as foo

fun test(list: List<String>) {
    list.<caret>foo { index, _ ->
        index + 42
    }
}