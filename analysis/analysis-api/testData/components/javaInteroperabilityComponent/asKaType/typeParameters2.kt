// FILE: B.kt
class AK: A<String>() {
    inner class BK: B<String>() {
        override fun <K : String?> foo(): K {
        }
    }
}
// FILE: A.java
class A<T> {
    abstract class B<S extends T> {
        abstract <K extends S> K f<caret>oo();
    }
}