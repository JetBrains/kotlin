// FILE: main.kt
import java.lang.* // will not import Fake

fun foo() {
    <caret>Fake()
}

// FILE: java.java
class java {
    class lang {
        class Fake()
    }
}
