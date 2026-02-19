// FILE: DeclSite.java

public class DeclSite {
    private int x;

    public void setX(int value) {
        x = value;
    }

    public int getX() {
        return x;
    }
}

// FILE: UseSite.kt

fun test(declSite: DeclSite) {
    declSite.<caret>x = 0
}