import testData.libraries.*

fun foo() {
    WithInnerAndObject.foo()
}

// library.kt
//public class <1>WithInnerAndObject {
//    class object {
//        fun <2>foo() {