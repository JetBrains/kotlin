// "Make primary constructor parameter 'bar' a property in class 'B'" "true"

class B(bar: String) {

    inner class A {
        fun foo() {
            val a = bar<caret>
        }
    }
}