// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

// FILE: TestCase1.kt
// TESTCASE NUMBER: 1
package testsCase1

open class A {
    operator fun invoke() = print("invoke")
}

interface Super_0 {
    companion object values : A()

    private fun case() {
        <!DEBUG_INFO_CALL("fqName: testsCase1.A.invoke; typeCall: variable&invoke")!>values()<!>
    }
}

open class Super_1 : Super_0 {
    companion object values : A() {}

    private fun case() {
        <!DEBUG_INFO_CALL("fqName: testsCase1.A.invoke; typeCall: variable&invoke")!>values()<!>
    }
}

open class Super_2 : Super_1() {

    private fun case() {
        <!DEBUG_INFO_CALL("fqName: testsCase1.A.invoke; typeCall: variable&invoke")!>values()<!>
    }

    class Nested : Super_1() {
        companion object values : A() {}

        private fun case() {
            <!DEBUG_INFO_CALL("fqName: testsCase1.A.invoke; typeCall: variable&invoke")!>values()<!>
        }
    }

    inner class Inner : Super_1() {
        private fun case() {
            <!DEBUG_INFO_CALL("fqName: testsCase1.A.invoke; typeCall: variable&invoke")!>values()<!>
        }
    }

}


// FILE: TestCase2.kt
// TESTCASE NUMBER: 2
package testsCase2

open class A {
    operator fun invoke(value: String) = print("invoke $value")
}

interface Super_0 {
    companion object valueOf : A()

    private fun case() {
        <!DEBUG_INFO_CALL("fqName: testsCase2.A.invoke; typeCall: variable&invoke")!>valueOf("")<!>
    }
}

open class Super_1 : Super_0 {
    companion object valueOf : A() {}

    private fun case() {
        <!DEBUG_INFO_CALL("fqName: testsCase2.A.invoke; typeCall: variable&invoke")!>valueOf("")<!>
    }
}

open class Super_2 : Super_1() {

    private fun case() {
        <!DEBUG_INFO_CALL("fqName: testsCase2.A.invoke; typeCall: variable&invoke")!>valueOf("")<!>
    }

    class Nested : Super_1() {
        companion object valueOf : A() {}

        private fun case() {
            <!DEBUG_INFO_CALL("fqName: testsCase2.A.invoke; typeCall: variable&invoke")!>valueOf("")<!>
        }
    }

    inner class Inner : Super_1() {
        private fun case() {
            <!DEBUG_INFO_CALL("fqName: testsCase2.A.invoke; typeCall: variable&invoke")!>valueOf("")<!>
        }
    }

}

// FILE: TestCase3.kt
// TESTCASE NUMBER: 3
package testsCase3

open class A {
    operator fun invoke() = print("invoke")
}

interface Super_0 {
    object values : A()

    private fun case() {
        <!DEBUG_INFO_CALL("fqName: testsCase3.A.invoke; typeCall: variable&invoke")!>values()<!>
    }
}

open class Super_1 : Super_0 {
    object values : A() {}

    private fun case() {
        <!DEBUG_INFO_CALL("fqName: testsCase3.A.invoke; typeCall: variable&invoke")!>values()<!>
    }
}

open class Super_2 : Super_1() {

    private fun case() {
        <!DEBUG_INFO_CALL("fqName: testsCase3.A.invoke; typeCall: variable&invoke")!>values()<!>
    }

    class Nested : Super_1() {
        object values : A() {}

        private fun case() {
            <!DEBUG_INFO_CALL("fqName: testsCase3.A.invoke; typeCall: variable&invoke")!>values()<!>
        }
    }

    inner class Inner : Super_1() {
        private fun case() {
            <!DEBUG_INFO_CALL("fqName: testsCase3.A.invoke; typeCall: variable&invoke")!>values()<!>
        }
    }

}


// FILE: TestCase4.kt
// TESTCASE NUMBER: 4
package testsCase4

open class A {
    operator fun invoke(value: String) = print("invoke $value")
}

interface Super_0 {
    object valueOf : A()

    private fun case() {
        <!DEBUG_INFO_CALL("fqName: testsCase4.A.invoke; typeCall: variable&invoke")!>valueOf("")<!>
    }
}

open class Super_1 : Super_0 {
    object valueOf : A() {}

    private fun case() {
        <!DEBUG_INFO_CALL("fqName: testsCase4.A.invoke; typeCall: variable&invoke")!>valueOf("")<!>
    }
}

open class Super_2 : Super_1() {

    private fun case() {
        <!DEBUG_INFO_CALL("fqName: testsCase4.A.invoke; typeCall: variable&invoke")!>valueOf("")<!>
    }

    class Nested : Super_1() {
        object valueOf : A() {}

        private fun case() {
            <!DEBUG_INFO_CALL("fqName: testsCase4.A.invoke; typeCall: variable&invoke")!>valueOf("")<!>
        }
    }

    inner class Inner : Super_1() {
        private fun case() {
            <!DEBUG_INFO_CALL("fqName: testsCase4.A.invoke; typeCall: variable&invoke")!>valueOf("")<!>
        }
    }

}
