package test

import java.util.*

open class CustomerService {

    fun <T> comparator() = object : Comparator<T> {
        override fun compare(o1: T, o2: T): Int {
            throw UnsupportedOperationException()
        }
    }

    inline fun <T> comparator(crossinline z: () -> Int) = object : Comparator<T> {

        override fun compare(o1: T, o2: T): Int {
            return z()
        }

    }

    fun callInline() =  comparator<String> { 1 }

}

