// DO_NOT_CHECK_SYMBOL_RESTORE_K1
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
