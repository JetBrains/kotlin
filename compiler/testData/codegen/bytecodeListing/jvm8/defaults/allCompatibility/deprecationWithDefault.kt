// !JVM_DEFAULT_MODE: all-compatibility
// JVM_TARGET: 1.8
// WITH_STDLIB

interface Deprecated {
    @java.lang.Deprecated
    fun test() {
    }

    val prop: String
        @java.lang.Deprecated get() = "123"

    @java.lang.Deprecated
    fun withDefault(s: String = "123") {

    }
}
