// EXTRACTION_TARGET: property with getter
fun foo(n: Int): Int {
    return {<selection>n + 1</selection>}()
}