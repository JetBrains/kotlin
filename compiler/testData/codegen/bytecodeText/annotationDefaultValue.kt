// JVM_ABI_K1_K2_DIFF: K2 serializes annotation parameter default values (KT-59526).

annotation class Ann(val arg: String = "abc")

@Ann class MyClass

// 1 @LAnn;\(\)
