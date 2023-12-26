// JVM_ABI_K1_K2_DIFF: KT-63984

// FILE: Dummy.kt
// Empty body to trigger multifile test mode

// FILE: Test.kt
class TestMethod {
    companion object {
        @JvmStatic
        fun test(s0: String, s1: String?) = s0 + (s1 ?: "null")
    }
}

class TestMethodOverloads {
    companion object {
        @JvmStatic
        @JvmOverloads
        fun test(s0: String = "s0", s1: String = "s1", s2: String = "s2") = s0 + s1 + s2
    }
}

class TestProperty {
    companion object {
        @JvmStatic
        var prop: String = "Blah"
    }
}

class TestAccessor {
    companion object {
        var prop: String = "Blah" @JvmStatic set
    }
}

// @TestMethod.class:
// 0 INVOKESTATIC kotlin/jvm/internal/Intrinsics.checkParameterIsNotNull \(Ljava/lang/Object;Ljava/lang/String;\)V
// 0 INVOKESTATIC kotlin/jvm/internal/Intrinsics.checkNotNullParameter \(Ljava/lang/Object;Ljava/lang/String;\)V

// @TestMethodOverloads.class:
// 0 INVOKESTATIC kotlin/jvm/internal/Intrinsics.checkParameterIsNotNull \(Ljava/lang/Object;Ljava/lang/String;\)V
// 0 INVOKESTATIC kotlin/jvm/internal/Intrinsics.checkNotNullParameter \(Ljava/lang/Object;Ljava/lang/String;\)V

// @TestProperty.class:
// 0 INVOKESTATIC kotlin/jvm/internal/Intrinsics.checkParameterIsNotNull \(Ljava/lang/Object;Ljava/lang/String;\)V
// 0 INVOKESTATIC kotlin/jvm/internal/Intrinsics.checkNotNullParameter \(Ljava/lang/Object;Ljava/lang/String;\)V

// @TestAccessor.class:
// 0 INVOKESTATIC kotlin/jvm/internal/Intrinsics.checkParameterIsNotNull \(Ljava/lang/Object;Ljava/lang/String;\)V
// 0 INVOKESTATIC kotlin/jvm/internal/Intrinsics.checkNotNullParameter \(Ljava/lang/Object;Ljava/lang/String;\)V
