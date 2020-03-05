// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * PLACE: expressions, try-expression -> paragraph 1 -> sentence 4
 * RELEVANT PLACES: expressions, try-expression -> paragraph 1 -> sentence 2
 * NUMBER: 2
 * DESCRIPTION: finally is a soft keyword
 */
fun throwException(): Nothing = throw Exception()

// TESTCASE NUMBER: 1
class Case1 {
    fun finally(e: Exception) {}

    fun case1() {
        finally(Exception())
    }
}

// TESTCASE NUMBER: 2
class Case2 {
    class finally(e: Exception) {}

    fun case2() {
        val c = finally(java.lang.Exception())
    }
}

// TESTCASE NUMBER: 3
class Case3 {
    fun finally() {}

    fun case3() {
        finally()
    }
}

// TESTCASE NUMBER: 4
class Case4 {
    class finally() {}

    fun case4() {
        val c = finally()
    }
}

// TESTCASE NUMBER: 5

class Case5() {
    interface finally

    fun case5(){
        val c = object :finally{}
    }
}

