package test

interface KotlinInterface {
    fun bar() {

    }

    fun f()
}


abstract class KotlinClass : KotlinInterface {
    override fun f() {

    }
}