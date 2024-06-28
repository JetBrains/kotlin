// FIR_IDENTICAL
// KT-7753: attempt to call enum constructor explicitly
enum class A(val c: Int) {
    ONE(1) {
        override fun selfOrFriend(): A {
            return this
        }
    },
    TWO(2) {
        override fun selfOrFriend(): A {
            return <!ENUM_CLASS_CONSTRUCTOR_CALL!>A(42)<!>
        }
    };
    
    abstract fun selfOrFriend(): A
}