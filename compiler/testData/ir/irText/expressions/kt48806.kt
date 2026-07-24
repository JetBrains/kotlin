// ISSUE: KT-48806
// DUMP_IR_DIFFERENCE: JVM
//  K/JVM throws java.lang.RuntimeException instead of kotlin.RuntimeException,  and catches java.lang.Exception instead of kotlin.Exception

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

