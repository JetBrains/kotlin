package syntheticProvider

interface I {
    fun manyInts(i: Int, i2: Int, i3: Int, i4: Int): Int {
        return i
    }

    fun manyLongs(i: Long, i2: Long, i3: Long, i4: Long): Long {
        return i
    }

    fun manyDouble(i: Double, i2: Double, i3: Double, i4: Double): Double {
        return i
    }

    fun manyFloat(i: Float, i2: Float, i3: Float, i4: Float): Float {
        return i
    }

    fun manyObject(i: Any, i2: Any, i3: Any, i4: Any): Any {
        return i
    }

    fun void() {
        val a = 1
    }
}

class IImpl: I

fun main(args: Array<String>) {
    val i = IImpl()
    i.manyInts(1, 1, 1, 1)

    // STEP_INTO: 1
    // RESUME: 1
    //Breakpoint!
    i.manyInts(1, 1, 1, 1)
    // STEP_INTO: 1
    // RESUME: 1
    //Breakpoint!
    i.manyLongs(1, 1, 1, 1)
    // STEP_INTO: 1
    // RESUME: 1
    //Breakpoint!
    i.manyDouble(1.0, 1.0, 1.0, 1.0)
    // STEP_INTO: 1
    // RESUME: 1
    //Breakpoint!
    i.manyFloat(1.0f, 1.0f, 1.0f, 1.0f)
    // STEP_INTO: 1
    // RESUME: 1
    //Breakpoint!
    i.manyObject(1, 1, 1, 1)
    // STEP_INTO: 1
    // RESUME: 1
    //Breakpoint!
    i.void()
}