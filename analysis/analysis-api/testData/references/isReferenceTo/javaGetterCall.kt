// FILE: DeclSite.java

public class DeclSite {
    private int x;

    public int getX() {
        return x;
    }
}

// FILE: UseSite.kt

fun test(declSite: DeclSite) {
    declSite.<caret>x
}