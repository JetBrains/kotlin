// "Create member function 'foo'" "true"
// ERROR: Unresolved reference: foo

class A<T>(val b: B<T>) {
    fun test(): Int {
        return b.<caret>foo<Int, String>(2, "2")
    }
}