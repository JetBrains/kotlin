// FILE: X.java

public class X {
    int getFoo() {return 3;}
}

// FILE: Usage.kt

class A : X() {
    // TODO: DEBUG_INFO_MISSING_UNRESOLVED indicates a bug here
    override fun getFoo() = foo
}