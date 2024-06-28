// FILE: main.kt
package nonRoot

fun foo() {
    java.lang.<caret>Fake() // qualification doesn't help, because we are in other package
}


// FILE: java.java
class java {
    class lang {
        class Fake()
    }
}
