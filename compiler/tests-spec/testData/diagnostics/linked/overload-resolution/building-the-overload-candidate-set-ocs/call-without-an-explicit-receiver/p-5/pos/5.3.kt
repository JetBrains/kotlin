// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION -NOTHING_TO_INLINE
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-278
 * MAIN LINK: overload-resolution, building-the-overload-candidate-set-ocs, call-without-an-explicit-receiver -> paragraph 5 -> sentence 5
 * PRIMARY LINKS: overload-resolution, building-the-overload-candidate-set-ocs, call-without-an-explicit-receiver -> paragraph 4 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, call-without-an-explicit-receiver -> paragraph 8 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, call-without-an-explicit-receiver -> paragraph 6 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, call-with-specified-type-parameters -> paragraph 1 -> sentence 2
 * NUMBER: 3
 * DESCRIPTION: Top-level non-extension functions: Callables declared in the same package
 */

// FILE: TestCase.kt
// TESTCASE NUMBER: 1
package testsCase1

fun case1() {
    emptyArray<Int>() //  to (1)
    <!DEBUG_INFO_CALL("fqName: testsCase1.emptyArray; typeCall: function")!>emptyArray<Int>()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Array<kotlin.Int>")!>emptyArray<Int>()<!>

    String()//to (2)
    <!DEBUG_INFO_CALL("fqName: testsCase1.String.<init>; typeCall: function")!>String()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("testsCase1.String")!>String()<!>

    val x = object : Map<Int, Int>{ } //to (3)

    run {  } // to (4)
    <!DEBUG_INFO_CALL("fqName: testsCase1.run; typeCall: inline function")!>run {  }<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("testsCase1.String")!>run {  }<!>
}

// FILE: LibtestsPack.kt
package testsCase1

public fun <T> emptyArray(): Array<T> = TODO() //(1)
public class String() //(2)
public interface Map<K, out V> //(3)
public inline fun <R> run(block: () -> R): String = TODO() //(4)

