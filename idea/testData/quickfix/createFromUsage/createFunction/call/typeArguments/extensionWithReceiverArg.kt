// "Create extension function 'List<T>.foo'" "true"
// WITH_RUNTIME

class A<T>(val items: List<T>) {
    fun test(): Int {
        return items.<caret>foo<T, Int, String>(2, "2")
    }
}