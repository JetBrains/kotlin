// !LANGUAGE: +EnumEntries
// WITH_STDLIB

package pckg

val entries = "E"

enum class E {
    ;

    fun foo() {
        entries.<!UNRESOLVED_REFERENCE!>length<!>
        pckg.entries.length
    }
}

class A {
    enum class E {
        ;

        class B {
            fun foo() {
                entries.<!UNRESOLVED_REFERENCE!>length<!>
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
