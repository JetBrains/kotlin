// FILE: B.kt
class AK: A<String>() {
}
// FILE: A.java
class A<T> {
    A<T> a = new A<>() {
        T f<caret>oo() {

        }
    }
}