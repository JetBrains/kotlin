// "Add '@UnsafeVariance' annotation" "true"
interface Foo<out E> {
    fun bar(e: E<caret>)
}