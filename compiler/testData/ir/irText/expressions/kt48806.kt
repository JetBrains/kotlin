// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// ISSUE: KT-48806

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

