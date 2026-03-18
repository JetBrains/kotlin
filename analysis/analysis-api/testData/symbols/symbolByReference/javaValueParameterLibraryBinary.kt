// DO_NOT_REQUIRE_SYMBOL_RESTORATION_K1
// MODULE: dependency
// MODULE_KIND: LibraryBinary
// FILE: JavaAnnotation.java
public @interface JavaAnnotation {
    String name();
}

// MODULE: main(dependency)
// FILE: main.kt
@JavaAnnotation(n<caret>ame = "")
fun test() {}
