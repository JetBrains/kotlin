// EXTRACT_AS_PROPERTY

fun foo(n: Int): Int {
    // SIBLING:
    return {<selection>n + 1</selection>}()
}