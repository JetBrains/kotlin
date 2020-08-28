// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT


// FILE: TestCase1.kt
/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 */
package testPackCase1
import libCase1.*
import kotlin.text.*

fun case1() {
    Regex("")
}

// FILE: Lib.kt
package libCase1
fun Regex(pattern: String) {}


// FILE: TestCase1.kt
/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 */
package testPackCase1
import kotlin.text.Regex

fun case() {
    <!DEBUG_INFO_CALL("fqName: kotlin.text.Regex.Regex; typeCall: function")!>Regex("")<!>
}

// FILE: Lib.kt
package testPackCase1
class Regex(pattern: String) {}



// FILE: TestCase2.kt
/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 */
package testPackCase2
import libCase2.a.*
import libCase2.b.*
import kotlin.text.*

fun case2() {
    <!DEBUG_INFO_CALL("fqName: libCase2.a.Regex; typeCall: function")!>Regex("")<!>
}

// FILE: Lib.kt
package libCase2.a
fun Regex(pattern: String) {}

//object Regex {
//    operator fun invoke(s: String) {}
//}

// FILE: Lib1.kt
package libCase2.b

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
 */
package testPackCase3
import libCase3.*
import kotlin.text.*

fun case3() {
    <!DEBUG_INFO_CALL("fqName: libCase3.Regex; typeCall: function")!>Regex("")<!>
}

// FILE: Lib.kt
package libCase3
fun Regex(pattern: String) {}

object Regex {
    operator fun invoke(s: String) {}
}


// FILE: TestCase4.kt
/*
 * TESTCASE NUMBER: 4
 * UNEXPECTED BEHAVIOUR
 */
package testPackCase4
import libCase4.a.*
import libCase4.b.*
import kotlin.text.*

fun case4() {
    <!DEBUG_INFO_CALL("fqName: libCase4.a.Regex; typeCall: function")!>Regex("")<!>
}

// FILE: Lib.kt
package libCase4.a
fun Regex(pattern: String) {}

object Regex {
    operator fun invoke(s: String) {}
}

// FILE: Lib1.kt
package libCase4.b

enum class Regex{
    ;

    companion object {
        operator fun invoke(s: String) {}
    }
}


// FILE: TestCase5.kt
/*
 * TESTCASE NUMBER: 5
 * UNEXPECTED BEHAVIOUR
 */
package testPackCase5

fun case() {
   <!DEBUG_INFO_CALL("fqName: testPackCase5.Regex; typeCall: function")!>Regex("")<!>
}

// FILE: Lib.kt
package testPackCase5
fun Regex(pattern: String) {}


// FILE: TestCase6.kt
/*
 * TESTCASE NUMBER: 6
 * UNEXPECTED BEHAVIOUR
 */
package testPackCase6
import kotlin.text.*

fun case() {
   <!DEBUG_INFO_CALL("fqName: testPackCase6.Regex; typeCall: function")!>Regex("")<!>
}

// FILE: Lib.kt
package testPackCase6
fun Regex(pattern: String) {}


// FILE: TestCase7.kt
/*
 * TESTCASE NUMBER: 7
 * UNEXPECTED BEHAVIOUR
 */
package testPackCase7
import kotlin.text.Regex

fun case() {
   <!DEBUG_INFO_CALL("fqName: kotlin.text.Regex.Regex; typeCall: function")!>Regex("")<!>
}

// FILE: Lib.kt
package testPackCase7
class Regex(pattern: String) {}



// FILE: TestCase8.kt
/*
 * TESTCASE NUMBER: 8
 * UNEXPECTED BEHAVIOUR
 */
package testPackCase8

fun case() {
    <!DEBUG_INFO_CALL("fqName: testPackCase8.Regex.Regex; typeCall: function")!>Regex("")<!>
}

// FILE: Lib.kt
package testPackCase8
class Regex(pattern: String) {}


// FILE: TestCase9.kt
/*
 * TESTCASE NUMBER: 9
 * UNEXPECTED BEHAVIOUR
 */
package testPackCase9
import kotlin.text.*

fun case() {
    <!DEBUG_INFO_CALL("fqName: testPackCase9.Regex.Regex; typeCall: function")!>Regex("")<!>
}

// FILE: Lib.kt
package testPackCase9
class Regex(pattern: String) {}
