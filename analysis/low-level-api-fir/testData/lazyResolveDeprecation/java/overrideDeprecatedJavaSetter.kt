// FILE: Kotlin.kt

class Kotlin : Java {
    override var aa<caret>a = 1
}

// FILE: Java.java

/**
 * @deprecated Deprecated
 */
class Java {
    int getAaa() {}

    void setAaa(int a){}
}