class A {
    companion object {
        @JvmStatic
        internal fun f(): String = "OK"
    }
}

fun box(): String {
    return A.f()
}

// Check the names of mangled functions
// 1 public final f\$test_module\(\)Ljava/lang/String;
// 1 public final static f\$test_module\(\)Ljava/lang/String;
