// FILE: DeclSite.java
public class DeclSite {
    private int foo;

    public void setFoo(int value) {
        foo = value;
    }

    public int getFoo() {
        return foo;
    }
}

// FILE: main.kt
fun test(declSite: DeclSite) {
    declSite.f<caret>oo = 0
}
