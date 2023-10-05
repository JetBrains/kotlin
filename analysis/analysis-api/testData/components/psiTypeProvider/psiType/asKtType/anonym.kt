// FILE: B.kt
class A<caret_onAirContext>K: A<String>() {
}
// FILE: A.java
class A<T> {
    A<T> a = new A<>() {
        T f<caret>oo() {

        }
    }
}