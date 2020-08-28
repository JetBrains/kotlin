// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT




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

// FILE: Lib.kt
package libCase1.a
fun Regex(pattern: String) {}

object Regex {
    operator fun invoke(s: String) {}
}

// FILE: Lib1.kt
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
 * ISSUES: KT-39162
 */
package testPackCase3
import libCase3.a.*
import libCase3.b.*

fun case3() {
    <!DEBUG_INFO_CALL("fqName: libCase3.a.Regex; typeCall: function")!>Regex("")<!>
}

// FILE: Lib.kt
package libCase3.a
fun Regex(pattern: String) {}

object Regex {
    operator fun invoke(s: String) {}
}

// FILE: Lib1.kt
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
    <!AMBIGUITY!>Regex<!>("")
}

// FILE: Lib.kt
package libCase4.a
fun Regex(pattern: String) {}

//object Regex {
//    operator fun invoke(s: String) {}
//}

// FILE: Lib1.kt
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
    <!AMBIGUITY!>A<!>()
}

// FILE: Lib.kt
package libCase5.a
fun A() {} //(1)
//object A{
//    operator fun invoke(){}
//}

// FILE: Lib.kt
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

// FILE: Lib.kt
package libCase6.a
fun A() : String = " " //(1)
object A{
    //operator fun invoke(){}
}
// FILE: Lib.kt
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
    <!AMBIGUITY!>A<!>()
}
object A{
    //operator fun invoke(){}
}
// FILE: Lib.kt
package libCase7.a
fun A() : String = " " //(1)

// FILE: Lib.kt
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
    <!DEBUG_INFO_CALL("fqName: fqName is unknown; typeCall: unresolved")!><!AMBIGUITY!>A<!>()<!>
    <!AMBIGUITY!>A<!>()
}

// FILE: Lib.kt
package libCase8.a
fun A() : String = " " //(1)

// FILE: Lib.kt
package libCase8.b
class A()
// FILE: Lib.kt
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
    <!AMBIGUITY!>A<!>()
}

// FILE: Lib.kt
package libCase9.a
fun A() : String = " " //(1)

// FILE: Lib.kt
package libCase9.b
class A()
// FILE: Lib.kt
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
    <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Ambiguity: A, [libCase10/b/A.A, libCase10/a/A]")!><!AMBIGUITY!>A<!>()<!>
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
