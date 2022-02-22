// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT


// FILE: TestCase1.kt
/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-39073
 */
package testPackCase1

import libCase1.*
import kotlin.text.*

fun case1() {
    <!DEBUG_INFO_CALL("fqName: kotlin.text.Regex.Regex; typeCall: function")!>Regex("")<!>
}

// FILE: Lib1.kt
package libCase1
class Regex(pattern: String)


// FILE: TestCase2.kt
/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-39073
 */
package testPackCase2
import libCase2.*
import lib1Case2.*
import kotlin.text.*

fun case2() {
    <!DEBUG_INFO_CALL("fqName: fqName is unknown; typeCall: unresolved")!><!OVERLOAD_RESOLUTION_AMBIGUITY!>Regex<!>("")<!>
}

// FILE: Lib2.kt
package libCase2
//fun Regex(pattern: String) {}

object Regex {
    operator fun invoke(s: String) {}
}

// FILE: Lib11.kt
package lib1Case2

enum class Regex{
    ;

    companion object {
        operator fun invoke(s: String) {}
    }
}

// FILE: TestCase3.kt
/*
 * TESTCASE NUMBER: 3
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-39073
 */
package testPackCase3
import libCase3.*
import kotlin.text.*

fun case3() {
    <!DEBUG_INFO_CALL("fqName: libCase3.Regex.Companion.invoke; typeCall: variable&invoke")!>Regex("")<!>
}

// FILE: Lib3.kt
package libCase3

enum class Regex{
    ;

    companion object {
        operator fun invoke(s: String) {}
    }
}


// FILE: TestCase4.kt
/*
 * TESTCASE NUMBER: 4
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-39073
 */
package testPackCase4
import libCase4.*
import lib1Case4.*
import kotlin.text.*

fun case4() {
    <!DEBUG_INFO_CALL("fqName: lib1Case4.Regex.Companion.invoke; typeCall: variable&invoke")!>Regex("")<!>
}

// FILE: Lib4.kt
package libCase4
class Regex(pattern: String) {}

// FILE: Lib12.kt
package lib1Case4

enum class Regex{
    ;

    companion object {
        operator fun invoke(s: String) {}
    }
}
