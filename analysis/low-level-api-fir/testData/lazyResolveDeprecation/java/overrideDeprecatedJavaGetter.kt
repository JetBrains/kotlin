// FILE: Kotlin.kt

class Kotlin : Java {
    override val aa<caret>a = 1
}

// FILE: Java.java

/**
 * @deprecated Deprecated
 */
class Java {
    @Deprecated
    int getAaa() {}
}