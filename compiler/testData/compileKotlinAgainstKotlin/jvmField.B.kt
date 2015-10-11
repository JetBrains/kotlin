open class C : B() {
    fun test(): String {
        return super.publicField + super.internalField + super.protectedfield
    }
}

fun main(args: Array<String>) {
    C().test()
}