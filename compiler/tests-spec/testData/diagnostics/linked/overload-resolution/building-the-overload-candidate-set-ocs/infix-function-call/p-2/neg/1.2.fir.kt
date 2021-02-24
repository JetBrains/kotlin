// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

// FILE: Extensions1.kt
package libPackage

operator fun CharSequence.contains(regex: Regex): Boolean {
    println("my contains")
    return true
}
// FILE: Extensions2.kt

package sentence3

operator fun CharSequence.contains(regex: Regex): Boolean {
    println("my package scope contains")
    return true
}

// FILE: TestCase1.kt
// TESTCASE NUMBER: 1

package sentence3
import libPackage.contains

class Case1() {
    operator fun CharSequence.contains(regex: Regex): Boolean {
        println("my local class scope contains")
        return true
    }

    fun case1() {
        val regex = Regex("")
        "" contains regex
    }
}
// FILE: TestCase2.kt
// TESTCASE NUMBER: 2

package sentence3
import libPackage.contains

interface Case2 {
    operator fun CharSequence.contains(regex: Regex): Boolean {
        println("my local interface scope contains")
        return true
    }

    fun case2() {
        val regex = Regex("")
        "" contains regex
    }
}

// FILE: TestCase3.kt
// TESTCASE NUMBER: 3
package testPack
import libPackage.contains

operator fun CharSequence.contains(regex: Regex): Boolean {
    println("my package scope top level contains")
    return true
}

fun case3() {
    operator fun CharSequence.contains(regex: Regex): Boolean {
        println("my package scope top level contains")
        return true
    }

    val regex = Regex("")
    "" contains regex
}

// FILE: TestCase4.kt
// TESTCASE NUMBER: 4
package testPackNew
import libPackage.contains

operator fun CharSequence.contains(regex: Regex): Boolean {
    println("my package scope top level contains")
    return true
}

fun case4() {

    operator fun CharSequence.contains(regex: Regex): Boolean {
        println("my local contains")
        return true
    }

    fun subfun() {

        operator fun CharSequence.contains(regex: Regex): Boolean {
            println("my local contains")
            return true
        }

        val regex = Regex("")
        "" contains regex

    }
}
