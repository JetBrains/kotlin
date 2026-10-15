// COMPILATION_ERRORS
package one.two

enum class {
    A,
    B {
        class InsideEntry
        object {
            class InsideNamelessInEntry
        }
    };

    class Nested
    object
}

object {
    enum class NamedEnum {
        C {
            class InsideEntry
        };

        class Nested
        object {
            class InsideNameless
        }
    }
}

class Outer {
    object {
        enum class {
            D;

            class Nested
        }
    }
}

fun foo() {
    enum class {
        E {
            class InsideEntry
        };

        class Nested
    }
}
