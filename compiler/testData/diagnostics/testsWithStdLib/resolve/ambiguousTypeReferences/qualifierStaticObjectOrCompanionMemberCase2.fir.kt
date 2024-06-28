// ISSUE: KT-56520 (case 7, object vs companion member vs static member, two companions in scope)
// FIR_DUMP

// FILE: some/Some.kt
package some

class Some {
    object foo {} // (1)

    companion object {}
}

// FILE: some2/Some.kt
package some2

class Some {
    companion object {
        val foo = "2" // (2)
    }
}

// FILE: some3/Some.java
package some3;

public class Some {
    public static int foo = 3; // (3)
}

// FILE: main.kt
import some.*
import some2.*
import some3.*

fun test() = <!OVERLOAD_RESOLUTION_AMBIGUITY!>Some<!>.<!UNRESOLVED_REFERENCE!>foo<!>
