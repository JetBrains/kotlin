// FILE: Kotlin.kt
import j.Java

class Kotlin : Java {
    override val aa<caret>a = 1
}

// FILE: j/Java.java

package j;

class Java {
    /**
     * @deprecated Deprecated
     */
    @Deprecated
    int getAaa() {}
}