package test

import kotlin.InlineOption.*

abstract class A<R> {
    abstract fun getO() : R

    abstract fun getK() : R

    abstract fun getParam() : R
}

inline fun <R> doWork(inlineOptions(ONLY_LOCAL_RETURN) jobO: ()-> R, inlineOptions(ONLY_LOCAL_RETURN) jobK: ()-> R, param: R) : A<R> {
    val s = object : A<R>() {

        override fun getO(): R {
            return jobO()
        }
        override fun getK(): R {
            return  jobK()
        }

        override fun getParam(): R {
            return param
        }
    }
    return s;
}

inline fun <R> doWorkInConstructor(inlineOptions(ONLY_LOCAL_RETURN) jobO: ()-> R, inlineOptions(ONLY_LOCAL_RETURN) jobK: ()-> R, param: R) : A<R> {
    val s = object : A<R>() {

        val p = param;

        val o1 = jobO()

        val k1 = jobK()

        override fun getO(): R {
            return o1
        }
        override fun getK(): R {
            return k1
        }

        override fun getParam(): R {
            return p
        }
    }
    return s;
}