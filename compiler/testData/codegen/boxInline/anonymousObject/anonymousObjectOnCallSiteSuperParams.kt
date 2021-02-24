// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt

package test

abstract class A<R>(val param: R) {
    abstract fun getO() : R

    abstract fun getK() : R
}


inline fun <R> doWork(job: ()-> R) : R {
    return job()
}

// FILE: 2.kt

import test.*

fun box() : String {
    val o = "O"
    val result = doWork {
        val k = "K"
        val s = object : A<String>("11") {
            override fun getO(): String {
                return o;
            }

            override fun getK(): String {
                return k;
            }
        }

        s.getO() + s.getK() + s.param
    }

    if (result != "OK11") return "fail $result"

    return "OK"
}
