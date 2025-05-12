// FILE: DeclSite.java

public class DeclSite {
    private int x;

    public DeclSite setX(int value) {
        x = value;
        return this;
    }

    public int getX() {
        return x;
    }
}

// FILE: UseSite.kt

fun test(declSite: DeclSite) {
    declSite.<caret>x = 0
}