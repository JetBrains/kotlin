// !LANGUAGE: +EnumEntries -PrioritizedEnumEntries
// WITH_STDLIB
// FIR_DUMP

package pckg

val entries = "E"

enum class E {
    ;

    fun foo() {
        <!DEPRECATED_ACCESS_TO_ENTRY_PROPERTY_FROM_ENUM!>entries<!>.length
        pckg.entries.length
    }
}

class A {
    enum class E {
        ;

        class B {
            fun foo() {
                <!DEPRECATED_ACCESS_TO_ENTRY_PROPERTY_FROM_ENUM!>entries<!>.length
                pckg.entries.length
            }
        }

        class C {
            val entries = 0

            fun foo() {
                // technically, this warning is incorrect but I believe it's OK to report anyway
                // first, logic in the compiler will be complicated if we'll try to avoid reporting warnings here
                // second, this code smells,  it'd be better to use qualifiers here anyway
                entries + 4
                this.entries + 4
            }
        }
    }
}
