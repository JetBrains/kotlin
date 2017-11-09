package test

interface KotlinInterface {
    fun foo() {

    }

    fun bar() {

    }

    fun f()
}


abstract class KotlinClass : KotlinInterface {
    override fun f() {

    }
}