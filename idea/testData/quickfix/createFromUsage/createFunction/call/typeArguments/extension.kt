// "Create extension function 'foo'" "true"

class A<T>(val items: List<T>) {
    fun test(): Int {
        return items.<caret>foo<Int, String>(2, "2")
    }
}