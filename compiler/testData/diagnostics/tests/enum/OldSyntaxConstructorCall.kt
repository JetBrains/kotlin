// KT-7753: attempt to call enum constructor explicitly
enum class A(val c: Int) {
    // No errors at both places, but warnings about deprecated
    ONE: <!ENUM_ENTRY_USES_DEPRECATED_SUPER_CONSTRUCTOR!>A(1)<!>,
    TWO: <!ENUM_ENTRY_USES_DEPRECATED_SUPER_CONSTRUCTOR!>A(2)<!>;
    
    fun getA(): A {
        return ONE
    }
}