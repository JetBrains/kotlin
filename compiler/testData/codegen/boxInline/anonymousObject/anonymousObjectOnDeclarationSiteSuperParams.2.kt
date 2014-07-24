package test

import kotlin.InlineOption.*

abstract class A<R>(val param : R) {
    abstract fun getO() : R

    abstract fun getK() : R
}

inline fun <R> doWork(inlineOptions(ONLY_LOCAL_RETURN) jobO: ()-> R, inlineOptions(ONLY_LOCAL_RETURN) jobK: ()-> R, param: R) : A<R> {
    val s = object : A<R>(param) {

        override fun getO(): R {
            return jobO()
        }
        override fun getK(): R {
            return  jobK()
        }
    }
    return s;
}

inline fun <R> doWorkInConstructor(inlineOptions(ONLY_LOCAL_RETURN) jobO: ()-> R, inlineOptions(ONLY_LOCAL_RETURN) jobK: ()-> R, inlineOptions(ONLY_LOCAL_RETURN) param: () -> R) : A<R> {
    val s = object : A<R>(param()) {
        val o1 = jobO()

        val k1 = jobK()

        override fun getO(): R {
            return o1
        }
        override fun getK(): R {
            return k1
        }
    }
    return s;
}