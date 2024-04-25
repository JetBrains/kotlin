// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-401
 * MAIN LINK: overload-resolution, building-the-overload-candidate-set-ocs, call-without-an-explicit-receiver -> paragraph 5 -> sentence 6
 * NUMBER: 4
 * DESCRIPTION:
 */



// FILE: TestCase1.kt
/*
 * TESTCASE NUMBER: 1
 *
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-39162
 */
package testPackCase1
import libCase1.a.*
import libCase1.b.*

fun case1() {
    <!DEBUG_INFO_CALL("fqName: libCase1.a.Regex; typeCall: function")!>Regex("")<!>
}

// FILE: Lib1.kt
package libCase1.a
fun Regex(pattern: String) {}

object Regex {
    operator fun invoke(s: String) {}
}

// FILE: Lib1_1.kt
package libCase1.b

enum class Regex{
    ;

    companion object {
        operator fun invoke(s: String) {}
    }
}




// FILE: TestCase2.kt
/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-39162
 */
package testPackCase2
import libCase2.a.*
import libCase2.b.*

fun case2() {
    <!DEBUG_INFO_CALL("fqName: libCase2.a.Regex; typeCall: function")!>Regex("")<!>
}

// FILE: Lib2.kt
package libCase2.a
fun Regex(pattern: String) {}

//object Regex {
//    operator fun invoke(s: String) {}
//}

// FILE: Lib12.kt
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
 * ISSUES: KT-39162
 */
package testPackCase3
import libCase3.a.*
import libCase3.b.*

fun case3() {
    <!DEBUG_INFO_CALL("fqName: libCase3.a.Regex; typeCall: function")!>Regex("")<!>
}

// FILE: Lib3.kt
package libCase3.a
fun Regex(pattern: String) {}

object Regex {
    operator fun invoke(s: String) {}
}

// FILE: Lib1_3.kt
package libCase3.b

class Regex(val s: String)


// FILE: TestCase4.kt
/*
 * TESTCASE NUMBER: 4
 */
package testPackCase4
import libCase4.a.*
import libCase4.b.*

fun case4() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>Regex<!>("")
}

// FILE: Lib4.kt
package libCase4.a
fun Regex(pattern: String) {}

//object Regex {
//    operator fun invoke(s: String) {}
//}

// FILE: Lib14.kt
package libCase4.b

class Regex(val s: String)


// FILE: TestCase5.kt
/*
 * TESTCASE NUMBER: 5
 */
package testPackCase5
import libCase5.a.*
import libCase5.b.*

fun case(){
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>A<!>()
}

// FILE: Lib5.kt
package libCase5.a
fun A() {} //(1)
//object A{
//    operator fun invoke(){}
//}

// FILE: Lib6.kt
package libCase5.b
class A()

// FILE: TestCase6.kt
/*
 * TESTCASE NUMBER: 6
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-39162
 */
package testPackCase6
import libCase6.a.*
import libCase6.b.*

fun case(){
    <!DEBUG_INFO_CALL("fqName: libCase6.a.A; typeCall: function")!>A()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>A()<!>
}

// FILE: Lib7.kt
package libCase6.a
fun A() : String = " " //(1)
object A{
    //operator fun invoke(){}
}
// FILE: Lib8.kt
package libCase6.b
class A()

// FILE: TestCase7.kt
/*
 * TESTCASE NUMBER: 7
 */
package testPackCase7
import libCase7.a.*
import libCase7.b.*

fun case7(){
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>A<!>()
}
object A{
    //operator fun invoke(){}
}
// FILE: Lib9.kt
package libCase7.a
fun A() : String = " " //(1)

// FILE: Lib10.kt
package libCase7.b
class A()

// FILE: TestCase8.kt
/*
 * TESTCASE NUMBER: 8
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-39162
 */
package testPackCase8
import libCase8.a.*
import libCase8.b.*
import libCase8.c.*

fun case8(){
    <!DEBUG_INFO_CALL("fqName: libCase8.a.A; typeCall: function")!>A()<!>
    A()
}

// FILE: Lib11.kt
package libCase8.a
fun A() : String = " " //(1)

// FILE: Lib1_2.kt
package libCase8.b
class A()
// FILE: Lib13.kt
package libCase8.c
object A{
    //operator fun invoke(){}
}


// FILE: TestCase9.kt
/*
 * TESTCASE NUMBER: 9
 */
package testPackCase
import libCase9.a.*
import libCase9.b.*
import libCase9.c.A

fun case9(){
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>A<!>()
}

// FILE: Lib1_4.kt
package libCase9.a
fun A() : String = " " //(1)

// FILE: Lib15.kt
package libCase9.b
class A()
// FILE: Lib16.kt
package libCase9.c
object A{
    //operator fun invoke(){}
}


// FILE: TestCase10.kt
/*
 * TESTCASE NUMBER: 10
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-39162
 */
package testPackCase
import libCase10.a.*
import libCase10.b.*
import libCase10.c.*

fun case10(){
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>A()<!>
}

// FILE: Liba.kt
package libCase10.a
fun A() : String = " " //(1)

// FILE: Libb.kt
package libCase10.b
class A()
// FILE: Libc.kt
package libCase10.c
interface A
