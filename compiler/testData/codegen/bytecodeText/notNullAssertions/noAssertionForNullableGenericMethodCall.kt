// KOTLIN_CONFIGURATION_FLAGS: +JVM.DISABLE_PARAM_ASSERTIONS

import java.util.HashMap

class A<T: Any> {
    fun main() {
        HashMap<String, T>()[""]
    }
}

// 0 kotlin/jvm/internal/Intrinsics
