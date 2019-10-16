package unboxParam

fun main(args: Array<String>) {
    val nullableInt = fooNullableInt()
    if (nullableInt == null) {
        return
    }

    // EXPRESSION: fooInt(nullableInt)
    // RESULT: 1: I
    //Breakpoint!
    val a = fooInt(nullableInt)
}

fun fooNullableInt(): Int? = 1

fun fooInt(param: Int) = param