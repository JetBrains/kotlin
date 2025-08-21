// JVM_DEFAULT_MODE: enable

interface Test {
    fun foo() = "abc"
    fun String.bar() = "def"
}

// 2 Intrinsics.checkNotNullParameter
//  ^ no null check for dispatch receiver parameters in 'foo' and 'bar',
//    extension receiver parameter in 'bar' is checked

// TODO: KT-75168 Do not generate parameter null checks on DefaultImpls methods in JVM default modes
