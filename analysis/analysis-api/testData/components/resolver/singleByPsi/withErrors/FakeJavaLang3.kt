// FILE: main.kt
package nonRoot
import java.lang.*

fun foo() {
    <caret>Fake()
}

// FILE: java.java
class java {
    class lang {
        class Fake()
    }
}
