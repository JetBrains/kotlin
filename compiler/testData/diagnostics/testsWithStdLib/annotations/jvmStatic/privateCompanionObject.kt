// Issue: KT-25114
// !DIAGNOSTICS: -UNUSED_PARAMETER

class WithPrivateCompanion {
    private companion object {
        <!JVM_STATIC_IN_PRIVATE_COMPANION!>@JvmStatic<!>
        val staticVal1: Int = 42

        val staticVal2: Int
            <!JVM_STATIC_IN_PRIVATE_COMPANION!>@JvmStatic<!> get() = 42

        <!JVM_STATIC_IN_PRIVATE_COMPANION!>@get:JvmStatic<!>
        val staticVal3: Int = 42

        <!JVM_STATIC_IN_PRIVATE_COMPANION!>@JvmStatic<!>
        var staticVar1: Int = 42

        var staticVar2: Int
            <!JVM_STATIC_IN_PRIVATE_COMPANION!>@JvmStatic<!> get() = 42
            <!JVM_STATIC_IN_PRIVATE_COMPANION!>@JvmStatic<!> set(value) {}

        <!JVM_STATIC_IN_PRIVATE_COMPANION!>@get: JvmStatic<!>
        <!JVM_STATIC_IN_PRIVATE_COMPANION!>@set: JvmStatic<!>
        var staticVar3: Int = 42

        <!JVM_STATIC_IN_PRIVATE_COMPANION!>@JvmStatic<!>
        fun staticFunction() {}
    }
}