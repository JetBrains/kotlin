// "Annotate property with @JvmField" "false"
import a.A;

class B {
    void bar() {
        A a = A.prop<caret>erty;
        A a2 = A.Named.getProperty();
        A a3 = A.property;
    }
}

// ACTION: Add 'const' modifier to a property
// ACTION: Add static import for 'a.A.property'
// ACTION: Annotate  'property' as @Deprecated
// ACTION: Annotate  'property' as @NotNull
// ACTION: Annotate  'property' as @Nullable
// ACTION: Change variable 'a' type to 'int'
// ACTION: Migrate 'a' type to 'int'
// ACTION: Replace with getter invocation
// ACTION: Split into declaration and assignment