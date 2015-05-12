package a

import java.lang.annotation.*

Retention(RetentionPolicy.RUNTIME)
annotation class Ann

interface Tr {
    Ann
    fun foo() {}
}