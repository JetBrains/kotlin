class simple_class_2_MyClass(val b: Int, var c: Int)

fun simple_class_2_genMyClass(i: Int): simple_class_2_MyClass {
    return simple_class_2_MyClass(i, i)
}

fun simple_class_2_change(x: Int): Int {
    val y = simple_class_2_MyClass(x, x)
    y.c = x
    y.c = x
    y.c = 1
    y.c = x + 1

    return y.c
}

fun simple_class_2_testGen(i: Int): Int {
    val j = simple_class_2_genMyClass(i)
    return j.b
}
