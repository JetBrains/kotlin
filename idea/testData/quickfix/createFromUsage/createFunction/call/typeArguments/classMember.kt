// "Create member function 'foo'" "true"

class B<T>(val t: T) {

}

class A<T>(val b: B<T>) {
    fun test(): Int {
        return b.<caret>foo<Int, String>(2, "2")
    }
}