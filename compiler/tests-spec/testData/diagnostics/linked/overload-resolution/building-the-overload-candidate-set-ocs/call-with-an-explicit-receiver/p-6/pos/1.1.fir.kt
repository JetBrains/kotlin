// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-268
 * MAIN LINK: overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 6 -> sentence 1
 * PRIMARY LINKS: overload-resolution, c-level-partition -> paragraph 1 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 3 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 2 -> sentence 1
 * overload-resolution, receivers -> paragraph 7 -> sentence 2
 * overload-resolution, receivers -> paragraph 7 -> sentence 3
 * NUMBER: 1
 * DESCRIPTION: sets of non-extension member callables only
 */

package tests.test2

// TESTCASE NUMBER: 1
open class Case1() {
    companion object foo {
        operator fun invoke() {}
    }

    val propVal: Int = 1
    val propVar: Int = 1
    fun function() {}
    val markerObj = object : Marker {}

    fun innerFun() {
        foo()
        Case1.foo()
        propVal()
        this.propVal()
        propVar()
        this.propVar()
        function()
        this.function()
        markerObj()
        this.markerObj()
    }

    operator fun Int.invoke() {}

    inner class InnerClass {

        fun innerClassFun() {
            foo()
            Case1.foo()

            propVal()
            this@Case1.propVal()

            propVar()
            this@Case1.propVar()

            function()
            this@Case1.function()

            markerObj()
            this@Case1.markerObj()
        }
    }
}

interface Marker {
    operator fun invoke() {}
}

class ExtendCase1() : Case1() {

    fun extFun() {
        foo()
        Case1.foo()
        propVal()
        this.propVal()
        propVar()
        this.propVar()
        function()
        this.function()
        markerObj()
        this.markerObj()
    }
}

fun case6() {
    Case1.invoke()
    Case1.foo()
}
