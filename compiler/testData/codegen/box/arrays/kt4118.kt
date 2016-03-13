fun Array<String>.test1(): Array<String> {
    val func = { i:Int  -> this}
    return func(1)
}

fun Array<String>.test1Nested(): Array<String> {
    val func = { i: Int -> { this }()}
    return func(1)
}


fun Array<String>.test2() : Array<String> {
    class Z2() {
        fun run(): Array<String> {
            return this@test2
        }
    }
    return Z2().run()
}

fun Array<String>.test2Nested() : Array<String> {
    class Z2() {
        fun run(): Array<String> {
            class Z3 {
                fun run(): Array<String> {
                   return this@test2Nested;
                }
            }
            return Z3().run()
        }
    }
    return Z2().run()
}

fun Array<String>.test3(): Array<String> {
    fun local(): Array<String> {
        return this@test3
    }
    return local()
}

fun Array<String>.test3Nested(): Array<String> {
    fun local(): Array<String> {
        fun local2(): Array<String> {
            return this@test3Nested
        }
        return local2()
    }
    return local()
}


fun Array<String>.test4() : Array<String> {
    return object {
                fun run() : Array<String> {
                    return this@test4
                }
            }.run()
}

fun Array<String>.test4Nested() : Array<String> {
    return object {
        fun run() : Array<String> {
            return object {
                fun run() : Array<String> {
                    return this@test4Nested
                }
            }.run()
        }
    }.run()
}

fun Array<DoubleArray>.test1(): Array<DoubleArray> {
    val func = { i: Int -> this}
    return func(1)
}


fun box() : String {
    val array = Array<String>(2, { i -> "${i}" })
    if (array != array.test1()) return "fail 1"
    if (array != array.test2()) return "fail 2"
    if (array != array.test3()) return "fail 3"
    if (array != array.test4()) return "fail 4"

    if (array != array.test1Nested()) return "fail 1Nested"
    if (array != array.test2Nested()) return "fail 2Nested"
    if (array != array.test3Nested()) return "fail 3Nested"
    if (array != array.test4Nested()) return "fail 4Nested"

    val array2 = Array<DoubleArray>(2, { i -> DoubleArray(i) })
    if (array2 != array2.test1()) return "fail on array of double []"
    return "OK"
}