import kotlin.collections.distinct as unique

fun foo() {
    listOf(1, 2, 3).<caret>
}

// ELEMENT: "unique"