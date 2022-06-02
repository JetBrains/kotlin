// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

// FILE: Extensions.kt
package libPackage

public fun String?.orEmpty(): String = "my Extension for $this"
/* a call with trailing lambda expression */
public fun String?.funWithTrailingLambda( x : Any? = null, body : ()-> String = {""} ): String = body()

// FILE: TestCase1.kt

package sentence3

import libPackage.*
import libPackage.orEmpty
import libPackage.funWithTrailingLambda

private fun String?.orEmpty(): String = "my top-level (pack scope) Extension for $this"
/* a call with trailing lambda expression */
private fun String?.funWithTrailingLambda( x : Any? = null, body : ()-> String = {""} ): String = body()

// TESTCASE NUMBER: 1
class Case1() {
    fun String?.orEmpty(): String = "my local extension for $this"
    fun String?.funWithTrailingLambda( x : Any? = null, body : ()-> String = {""} ): String = body()

    fun case1(s: String?) {
        s.<!DEBUG_INFO_CALL("fqName: sentence3.Case1.orEmpty; typeCall: extension function")!>orEmpty()<!>
        //trailing lambda
        s.<!DEBUG_INFO_CALL("fqName: sentence3.Case1.funWithTrailingLambda; typeCall: extension function")!>funWithTrailingLambda { "ss" }<!>
        s.<!DEBUG_INFO_CALL("fqName: sentence3.Case1.funWithTrailingLambda; typeCall: extension function")!>funWithTrailingLambda (x= 1) { "ss" }<!>
        s.<!DEBUG_INFO_CALL("fqName: sentence3.Case1.funWithTrailingLambda; typeCall: extension function")!>funWithTrailingLambda (body = { "ss" })<!>
        s.<!DEBUG_INFO_CALL("fqName: sentence3.Case1.funWithTrailingLambda; typeCall: extension function")!>funWithTrailingLambda (body = { "ss" }, x = '1')<!>
    }
}

// FILE: TestCase2.kt
package sentence3
import libPackage.*
//import libPackage.orEmpty

private fun String?.orEmpty(): String = "my top-level (pack scope) Extension for $this"
private fun String?.funWithTrailingLambda( x : Any? = null, body : ()-> String = {""} ): String = body()

// TESTCASE NUMBER: 2
class Case2() {
    fun String?.orEmpty(): String = "my local extension for $this"
    fun String?.funWithTrailingLambda( x : Any? = null, body : ()-> String = {""} ): String = body()

    fun case2(s: String?) {
        s.<!DEBUG_INFO_CALL("fqName: sentence3.Case2.orEmpty; typeCall: extension function")!>orEmpty()<!>

        //trailing lambda
        s.<!DEBUG_INFO_CALL("fqName: sentence3.Case2.funWithTrailingLambda; typeCall: extension function")!>funWithTrailingLambda { "ss" }<!>
        s.<!DEBUG_INFO_CALL("fqName: sentence3.Case2.funWithTrailingLambda; typeCall: extension function")!>funWithTrailingLambda (x= 1) { "ss" }<!>
        s.<!DEBUG_INFO_CALL("fqName: sentence3.Case2.funWithTrailingLambda; typeCall: extension function")!>funWithTrailingLambda (body = { "ss" })<!>
        s.<!DEBUG_INFO_CALL("fqName: sentence3.Case2.funWithTrailingLambda; typeCall: extension function")!>funWithTrailingLambda (body = { "ss" }, x = '1')<!>
    }
}

// TESTCASE NUMBER: 3
class Case3() {
    fun String?.orEmpty(): String = "my local extension for $this"
    fun String?.funWithTrailingLambda( x : Any? = null, body : ()-> String = {""} ): String = body()

    fun case3(s: String?) {
        s.<!DEBUG_INFO_CALL("fqName: sentence3.Case3.orEmpty; typeCall: extension function")!>orEmpty()<!>
        //trailing lambda
        s.<!DEBUG_INFO_CALL("fqName: sentence3.Case3.funWithTrailingLambda; typeCall: extension function")!>funWithTrailingLambda { "ss" }<!>
        s.<!DEBUG_INFO_CALL("fqName: sentence3.Case3.funWithTrailingLambda; typeCall: extension function")!>funWithTrailingLambda (x= 1) { "ss" }<!>
        s.<!DEBUG_INFO_CALL("fqName: sentence3.Case3.funWithTrailingLambda; typeCall: extension function")!>funWithTrailingLambda (body = { "ss" })<!>
        s.<!DEBUG_INFO_CALL("fqName: sentence3.Case3.funWithTrailingLambda; typeCall: extension function")!>funWithTrailingLambda (body = { "ss" }, x = '1')<!>

        fun innerFirst(s: String?){
            fun String?.orEmpty(): String = "my local inner extension for $this"
            fun String?.funWithTrailingLambda( x : Any? = null, body : ()-> String = {""} ): String = body()

            s?.<!DEBUG_INFO_CALL("fqName: sentence3.Case3.case3.innerFirst.orEmpty; typeCall: extension function")!>orEmpty()<!>
            //trailing lambda
            s.<!DEBUG_INFO_CALL("fqName: sentence3.Case3.case3.innerFirst.funWithTrailingLambda; typeCall: extension function")!>funWithTrailingLambda { "ss" }<!>
            s.<!DEBUG_INFO_CALL("fqName: sentence3.Case3.case3.innerFirst.funWithTrailingLambda; typeCall: extension function")!>funWithTrailingLambda (x= 1) { "ss" }<!>
            s.<!DEBUG_INFO_CALL("fqName: sentence3.Case3.case3.innerFirst.funWithTrailingLambda; typeCall: extension function")!>funWithTrailingLambda (body = { "ss" })<!>
            s.<!DEBUG_INFO_CALL("fqName: sentence3.Case3.case3.innerFirst.funWithTrailingLambda; typeCall: extension function")!>funWithTrailingLambda (body = { "ss" }, x = '1')<!>
        }
        fun innerSecond(){
            fun String?.orEmpty(): String = "my local inner extension for $this"
            fun String?.funWithTrailingLambda( x : Any? = null, body : ()-> String = {""} ): String = body()

            s.<!DEBUG_INFO_CALL("fqName: sentence3.Case3.case3.innerSecond.orEmpty; typeCall: extension function")!>orEmpty()<!>
            //trailing lambda
            s.<!DEBUG_INFO_CALL("fqName: sentence3.Case3.case3.innerSecond.funWithTrailingLambda; typeCall: extension function")!>funWithTrailingLambda { "ss" }<!>
            s.<!DEBUG_INFO_CALL("fqName: sentence3.Case3.case3.innerSecond.funWithTrailingLambda; typeCall: extension function")!>funWithTrailingLambda (x= 1) { "ss" }<!>
            s.<!DEBUG_INFO_CALL("fqName: sentence3.Case3.case3.innerSecond.funWithTrailingLambda; typeCall: extension function")!>funWithTrailingLambda (body = { "ss" })<!>
            s.<!DEBUG_INFO_CALL("fqName: sentence3.Case3.case3.innerSecond.funWithTrailingLambda; typeCall: extension function")!>funWithTrailingLambda (body = { "ss" }, x = '1')<!>
        }
        s.<!DEBUG_INFO_CALL("fqName: sentence3.Case3.orEmpty; typeCall: extension function")!>orEmpty()<!>
        //trailing lambda
        s.<!DEBUG_INFO_CALL("fqName: sentence3.Case3.funWithTrailingLambda; typeCall: extension function")!>funWithTrailingLambda { "ss" }<!>
        s.<!DEBUG_INFO_CALL("fqName: sentence3.Case3.funWithTrailingLambda; typeCall: extension function")!>funWithTrailingLambda (x= 1) { "ss" }<!>
        s.<!DEBUG_INFO_CALL("fqName: sentence3.Case3.funWithTrailingLambda; typeCall: extension function")!>funWithTrailingLambda (body = { "ss" })<!>
        s.<!DEBUG_INFO_CALL("fqName: sentence3.Case3.funWithTrailingLambda; typeCall: extension function")!>funWithTrailingLambda (body = { "ss" }, x = '1')<!>
    }
}
