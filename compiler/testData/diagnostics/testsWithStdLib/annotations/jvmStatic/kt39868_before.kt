// !LANGUAGE: -AllowJvmStaticOnProtectedCompanionObjectProperties
// FIR_IDENTICAL

// Ensure old behaviour without language feature enabled
class A {

    companion object {

        <!JVM_STATIC_ON_CONST_OR_JVM_FIELD!>@JvmStatic protected const val z<!> = 1;

        <!JVM_STATIC_ON_CONST_OR_JVM_FIELD!>@JvmStatic @JvmField protected val x<!> = 1;
    }

}
