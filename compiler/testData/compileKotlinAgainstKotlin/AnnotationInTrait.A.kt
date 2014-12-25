package a

import java.lang.annotation.*

Retention(RetentionPolicy.RUNTIME)
annotation class Ann

trait Tr {
    Ann
    fun foo() {}
}