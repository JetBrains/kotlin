// !JVM_TARGET: 1.8
// !JVM_DEFAULT_MODE: enable

interface B {

    @JvmDefault
    val prop1: String
    @JvmDefault get() = ""


    var prop2: String
        @JvmDefault get() = ""
        @JvmDefault set(value) {}
}
