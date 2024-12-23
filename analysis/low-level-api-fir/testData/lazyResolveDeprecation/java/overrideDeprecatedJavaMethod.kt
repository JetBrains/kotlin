// FILE: Kotlin.kt
import j.Java

class Kotlin : Java {
    override fun aa<caret>a(){}
}

// FILE: j/Java.java
package j;

class Java {
    /**
     * @deprecated Deprecated
     */
    @Deprecated
    void aaa() {}
}