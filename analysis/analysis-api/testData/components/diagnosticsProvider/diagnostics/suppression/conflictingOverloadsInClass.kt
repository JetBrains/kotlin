class SomeClass

class OtherClass {
    @Suppress("CONFLICTING_OVERLOADS")
    fun someFun(): SomeClass {
        return SomeClass()
    }

    @Suppress("CONFLICTING_OVERLOADS")
    fun someFun() {
    }
}
