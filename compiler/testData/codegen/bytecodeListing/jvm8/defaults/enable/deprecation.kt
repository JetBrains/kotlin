// JVM_DEFAULT_MODE: enable
// JVM_TARGET: 1.8
// WITH_STDLIB

interface Base {
    fun test() {}
    val prop: String
        get() = "123"

    fun withDefault(s: String = "123") {

    }
}

interface Derived: Base {

}

interface Deprecated {
    @java.lang.Deprecated
    fun test() {
    }

    val prop: String
        @java.lang.Deprecated get() = "123"

}
