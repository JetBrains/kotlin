// FILE: Kotlin.kt
import j.Java

class Kotlin : Java {
    override fun aa<caret>a(){}
}

// FILE: j/Java.java
package j
/**
 * @deprecated Deprecated
 */
class Java {
    @Deprecated
    void aaa() {}
}