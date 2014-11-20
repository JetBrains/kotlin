import java.lang.annotation.ElementType

fun foo(e: ElementType) {
    when(e) {
        ElementType.FIELD -> x()
        <caret>
    }
}

// ABSENT: FIELD
// EXIST: TYPE
// ABSENT: e
