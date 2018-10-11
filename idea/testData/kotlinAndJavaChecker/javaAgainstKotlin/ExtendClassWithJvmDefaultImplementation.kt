package test

interface KotlinInterface {
    @JvmDefault
    fun bar() {

    }

    @JvmDefault
    var foo: String
        get() = "123"
        set(field) {

        }
}


abstract class KotlinClass : KotlinInterface {

}