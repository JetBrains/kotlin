package test

import java.io.IOException

fun interface A<T> {
    @Throws(IOException::class)
    operator fun invoke(): T
}
