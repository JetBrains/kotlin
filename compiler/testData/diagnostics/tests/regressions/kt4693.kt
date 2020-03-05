// FIR_IDENTICAL
enum class MyEnum {
    K;
    
    inline fun doSmth(f: (MyEnum) -> String) : String {
        // This function should be inline
        return f(K)
    }
}