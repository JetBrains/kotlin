// EXTRACTION_TARGET: property with getter

open class Base(protected val i: Int)

class Impl(i: Int) : Base(i) {
    fun foo(): Int {
        return <selection>2 + 3 + i</selection>
    }
}