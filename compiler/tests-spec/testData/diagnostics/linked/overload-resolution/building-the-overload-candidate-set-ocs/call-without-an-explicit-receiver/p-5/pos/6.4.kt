// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ABSTRACT_MEMBER_NOT_IMPLEMENTED -UNUSED_PARAMETER
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-278
 * MAIN LINK: overload-resolution, building-the-overload-candidate-set-ocs, call-without-an-explicit-receiver -> paragraph 5 -> sentence 6
 * PRIMARY LINKS: overload-resolution, building-the-overload-candidate-set-ocs, call-without-an-explicit-receiver -> paragraph 4 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, call-without-an-explicit-receiver -> paragraph 8 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, call-without-an-explicit-receiver -> paragraph 6 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, call-with-specified-type-parameters -> paragraph 1 -> sentence 2
 * NUMBER: 4
 * DESCRIPTION: Top-level non-extension functions: callables star-imported into the current file
 * NOTE: ABSTRACT_MEMBER_NOT_IMPLEMENTED is muted
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-40839
 */

// FILE: TestCase.kt
// TESTCASE NUMBER: 1
package root1.testscase
import root1.*


fun case() {
    emptyArray<Int>() //  to (1)
    <!DEBUG_INFO_CALL("fqName: root1.emptyArray; typeCall: function")!>emptyArray<Int>()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Array<kotlin.Int>")!>emptyArray<Int>()<!>

    String()//to kotlin.String !!!
    <!DEBUG_INFO_CALL("fqName: kotlin.String.<init>; typeCall: function")!>String()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>String()<!>

    val x = object : Map<Int, Int>{ } //to kotlin.collections.Map !!!

    run {  } // to (4)
    <!DEBUG_INFO_CALL("fqName: root1.run; typeCall: inline function")!>run {  }<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!>run {  }<!>
}

// FILE: Lib.kt
package root1

public fun <T> emptyArray(): Array<T> = TODO() //(1)
public class String() //(2)
public interface Map<K, out V> { }//(3)
public inline fun <R> run(block: () -> R): R = TODO() //(4)


// FILE: TestCase.kt
// TESTCASE NUMBER: 2
package root2.testsCase2
import root2.lib.*

fun case() {
    emptyArray<Int>() //  to (1)
    <!DEBUG_INFO_CALL("fqName: root2.lib.emptyArray; typeCall: function")!>emptyArray<Int>()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Array<kotlin.Int>")!>emptyArray<Int>()<!>

    String()//to kotlin.String !!!
    <!DEBUG_INFO_CALL("fqName: kotlin.String.<init>; typeCall: function")!>String()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>String()<!>

    val x = object : Map<Int, Int>{} //to kotlin.collections.Map !!!

    run {  } // to (4)
    <!DEBUG_INFO_CALL("fqName: root2.lib.run; typeCall: inline function")!>run {  }<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!>run {  }<!>


}

// FILE: Lib.kt
package root2.lib

public fun <T> emptyArray(): Array<T> = TODO() //(1)
public class String() //(2)
public interface Map<K, out V> //(3)
public inline fun <R> run(block: () -> R): R = TODO() //(4)


// FILE: TestCase.kt
// TESTCASE NUMBER: 3
package testsCase3
import lib3.*

fun case() {
    emptyArray<Int>() //  to (1)
    <!DEBUG_INFO_CALL("fqName: lib3.emptyArray; typeCall: function")!>emptyArray<Int>()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Array<kotlin.Int>")!>emptyArray<Int>()<!>

    String()//to kotlin.String !!!
    <!DEBUG_INFO_CALL("fqName: kotlin.String.<init>; typeCall: function")!>String()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>String()<!>

    val x = object : Map<Int, Int>{} //to kotlin.collections.Map !!!

    run {  } // to (4)
    <!DEBUG_INFO_CALL("fqName: lib3.run; typeCall: inline function")!>run {  }<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!>run {  }<!>
}

// FILE: Lib.kt
package lib3

public fun <T> emptyArray(): Array<T> = TODO() //(1)
public class String() //(2)
public interface Map<K, out V> //(3)
public inline fun <R> run(block: () -> R): R = TODO() //(4)


// FILE: TestCase.kt
// TESTCASE NUMBER: 4
package testsCase4
import lib4.*

fun case() {
    emptyArray<Int>() //  to (1)
    <!DEBUG_INFO_CALL("fqName: lib4.emptyArray.<init>; typeCall: function")!>emptyArray<Int>()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("lib4.emptyArray<kotlin.Int>")!>emptyArray<Int>()<!>

    String()//to kotlin.String !!!
    <!DEBUG_INFO_CALL("fqName: kotlin.String.<init>; typeCall: function")!>String()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>String()<!>

    val x = object : Map<Int, Int>{} //to kotlin.collections.Map !!!

    run {  } // to (4)
    <!DEBUG_INFO_CALL("fqName: lib4.run.<init>; typeCall: function")!>run {  }<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("lib4.run<kotlin.Unit>")!>run {  }<!>
}

// FILE: Lib.kt
package lib4

public class emptyArray<T>() //(1)

public class String(val x : Any) {
    companion object{
        operator fun invoke(){} //(2)
    }
}
public class Map<K, out V> (val x : ()-> Unit) //(3)
public class run<R> (block: () -> R) //(4)
