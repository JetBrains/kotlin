package test

class outerClass<T>(val t: T) {
    inner class innerClass {
        fun getT() = t
    }
}

fun <T> outer(arg: T): T {
    class localClass(val v: T) {
        init {
            fun innerFunInLocalClass() = v

            val vv = innerFunInLocalClass()
        }
        fun member() = v
    }

    fun innerFun(): T {
        class localClassInLocalFunction {
            val v = arg
        }

        return localClass(arg).member()
    }

    fun <X> innerFunWithOwnTypeParam(x: X) = x

    innerFunWithOwnTypeParam(arg)
    return innerFun()
}