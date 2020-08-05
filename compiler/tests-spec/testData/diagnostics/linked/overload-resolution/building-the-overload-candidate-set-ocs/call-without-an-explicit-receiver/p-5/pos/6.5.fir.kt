// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION -NOTHING_TO_INLINE
// SKIP_TXT


// FILE: TestCase.kt
// TESTCASE NUMBER: 1
package root1.testsCase1
import root1.*

fun case2() {
    boo1<Int>() //  to (1)
    <!DEBUG_INFO_CALL("fqName: root1.boo1; typeCall: function")!>boo1<Int>()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Array<kotlin.Int>")!>boo1<Int>()<!>

    Boo2()//to (2)
    <!DEBUG_INFO_CALL("fqName: root1.Boo2.Boo2; typeCall: function")!>Boo2()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("root1.Boo2")!>Boo2()<!>

    val x = object : Boo3<Int, Int>{} //to (3)

    boo4 {  } // to (4)
    <!DEBUG_INFO_CALL("fqName: root1.boo4; typeCall: inline function")!>boo4 {  }<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!>boo4 {  }<!>

}

// FILE: LibtestsPack.kt
package root1

public fun <T> boo1(): Array<T> = TODO() //(1)
public class Boo2() //(2)
public interface Boo3<K, out V> //(3)
public inline fun <R> boo4(block: () -> R): R = TODO() //(4)


// FILE: TestCase.kt
// TESTCASE NUMBER: 2
package root2.testsCase2
import root2.lib.*

fun case2() {
    boo1<Int>() //  to (1)
    <!DEBUG_INFO_CALL("fqName: root2.lib.boo1; typeCall: function")!>boo1<Int>()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Array<kotlin.Int>")!>boo1<Int>()<!>

    Boo2()//to (2)
    <!DEBUG_INFO_CALL("fqName: root2.lib.Boo2.Boo2; typeCall: function")!>Boo2()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("root2.lib.Boo2")!>Boo2()<!>

    val x = object : Boo3<Int, Int>{} //to (3)

    boo4 {  } // to (4)
    <!DEBUG_INFO_CALL("fqName: root2.lib.boo4; typeCall: inline function")!>boo4 {  }<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!>boo4 {  }<!>

}

// FILE: LibtestsPack.kt
package root2.lib

public fun <T> boo1(): Array<T> = TODO() //(1)
public class Boo2() //(2)
public interface Boo3<K, out V> //(3)
public inline fun <R> boo4(block: () -> R): R = TODO() //(4)
