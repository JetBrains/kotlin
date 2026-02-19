// JVM_DEFAULT_MODE: disable

interface Test {
    fun foo() = "abc"
    fun String.bar() = "def"
}

// 1 Intrinsics.checkNotNullParameter
//  ^ no null check for dispatch receiver parameters in 'foo' and 'bar',
//    extension receiver parameter in 'bar' is checked
