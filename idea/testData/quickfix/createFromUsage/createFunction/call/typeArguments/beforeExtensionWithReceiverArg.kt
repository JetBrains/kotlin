// "Create function 'foo' from usage" "true"

class A<T>(val items: List<T>) {
    fun test(): Int {
        return items.<caret>foo<T, Int, String>(2, "2")
    }
}