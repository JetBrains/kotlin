// FILE: DeclSite.java

public class DeclSite {
    public static int calculateX() {
        return 0;
    }
}

// FILE: UseSite.kt

fun test() {
    <caret>DeclSite.calculateX()
}