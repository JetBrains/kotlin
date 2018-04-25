package test

interface KotlinInterface {
    @JvmDefault
    fun bar() {

    }

//    @JvmDefault
//    val foo: String
//        get() = "123"
}


abstract class KotlinClass : KotlinInterface {

}