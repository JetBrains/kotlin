// FULL_JDK

import java.util.*

fun foo(<!UNUSED_PARAMETER!>o<!>: Optional<String>) {}

class Test(nullable: String?) {
    private val nullableOptional = Optional.ofNullable(nullable)
    fun doIt() {
        foo(nullableOptional)
    }
}