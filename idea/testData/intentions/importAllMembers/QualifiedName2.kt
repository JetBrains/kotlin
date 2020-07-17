// INTENTION_TEXT: "Import members from 'java.util.Objects'"
// WITH_RUNTIME

fun foo() {
    <caret>java.util.Objects.equals(null, null)
}
