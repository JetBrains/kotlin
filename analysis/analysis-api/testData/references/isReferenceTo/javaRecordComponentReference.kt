// FILE: DeclSite.java

public record DeclSite(String fooBar) {
}

// FILE: UseSite.kt

fun test(d: DeclSite) {
    val fb = d.foo<caret>Bar
}