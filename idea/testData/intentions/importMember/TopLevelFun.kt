// INTENTION_TEXT: "Add import for 'kotlin.test.assertFailsWith'"
// WITH_RUNTIME

fun foo() {
    kotlin.test.<caret>assertFailsWith<Exception>("", {})
    kotlin.test.assertFailsWith(RuntimeException::class, "", {})
}
