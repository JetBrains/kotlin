// FILE: Kotlin.kt

class Kotlin : Java() {
    override var aa<caret>a = 1
}

// FILE: Java.java


class Java {
    int getAaa() {}

    /**
     * @deprecated Deprecated
     */
    @Deprecated
    void setAaa(int a){}
}