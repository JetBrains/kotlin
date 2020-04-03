// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * PLACE: expressions, try-expression -> paragraph 1 -> sentence 3
 * RELEVANT PLACES: expressions, try-expression -> paragraph 1 -> sentence 2
 * NUMBER: 2
 * DESCRIPTION: catch is a soft keyword
 */
fun throwException(): Nothing = throw Exception()

// TESTCASE NUMBER: 1
class Case1 {
    fun catch(e: Exception) {}

    fun case1() {
        catch(Exception())
    }
}

// TESTCASE NUMBER: 2
class Case2 {
    class catch(e: Exception) {}

    fun case2() {
        val c = catch(Exception())
    }
}

// TESTCASE NUMBER: 3
class Case3 {
    fun catch() {}

    fun case3() {
        catch()
    }
}

// TESTCASE NUMBER: 4
class Case4 {
    class catch() {}

    fun case4() {
        val c = catch()
    }
}

// TESTCASE NUMBER: 5

class Case5() {
    interface catch

    fun case5(){
        val c = object :catch{}
    }
}

