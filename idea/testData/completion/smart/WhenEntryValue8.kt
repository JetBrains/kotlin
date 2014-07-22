import java.lang.annotation.ElementType

fun foo(e: ElementType) {
    when(e) {
        ElementType.FIELD -> x()
        <caret>
    }
}

// ABSENT: ElementType.FIELD
// EXIST: ElementType.TYPE
// ABSENT: e
