// ISSUE: KT-56520 (case 7, enum entry vs companion member, one companion in scope)
// FIR_DUMP

// FILE: some/Some.kt
package some

enum class Some {
    foo; // (1)

    // companion object {}
}

// FILE: some2/Some.kt
package some2

enum class Some {
    foo; // (2)

    companion object {
        val foo = "2'" // (2')
    }
}

// FILE: some3/Some.java
package some3;

public enum Some {
    foo; // (3)
}

// FILE: main.kt
import some.*
import some2.*
import some3.*

fun test() = Some.foo