// FILE: main.kt

/**
 * @see ReferredContainer<caret_1>Java
 * @see ReferredContainer<caret_2>Java.ReferredClassifier
 * @see ReferredContainer<caret_3>Java.REFERRED_FIELD
 * @see ReferredContainer<caret_4>Java.referredMethod
 */
fun someFun() {
}

// FILE: ReferredContainerJava.java

public class ReferredContainerJava {
    public static int REFERRED_FIELD = 0;
    public static class ReferredClassifier {}
    public static void referredMethod() {}
}