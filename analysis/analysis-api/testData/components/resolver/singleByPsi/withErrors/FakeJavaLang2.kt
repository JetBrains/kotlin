// FILE: main.kt
fun foo() {
    <caret>Fake() // not imported within "java.lang.*" default import
}

// FILE: java.java
class java {
    class lang {
        class Fake()
    }
}


