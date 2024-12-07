// FILE: DeclSite.java

public class DeclSite {
    public int calculateX() {
        return 0;
    }
}

// FILE: UseSite.kt

fun test(declSite: DeclSite) {
    declSite.<caret>calculateX()
}