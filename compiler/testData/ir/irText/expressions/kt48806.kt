// FIR_IDENTICAL
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

