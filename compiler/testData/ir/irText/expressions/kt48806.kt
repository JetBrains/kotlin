// FIR_IDENTICAL
// IGNORE_BACKEND: JS_IR
// ISSUE: KT-48806

// KT-61141: throws kotlin.RuntimeException instead of java.lang.RuntimeException,  and catches kotlin.Exception instead of java.lang.Exception
// IGNORE_BACKEND: NATIVE

class A {
    val test_1: Int = try{
        throw RuntimeException()
    } catch(e: Exception) {
        1
    }

    val test_2: Int = try{
        1
    } catch(e: Exception) {
        throw RuntimeException()
    }
}

