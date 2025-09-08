// MODULE lib
// MODULE_KIND: LibraryBinaryDecompiled

// FILE: pckg/Outer.java
// BINARY_ROOT: first
package pckg;

class Outer {
    class AA {
    }
}

// FILE: pckg/Outer2.java
// BINARY_ROOT: second

package pckg;

class Outer {
    @Caret
    class AA {
        void correct(){}
    }
}

@interface Caret {}