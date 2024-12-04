// FIR_IDENTICAL
data class Test1<T>(val x: T)

data class Test2<T : Number>(val x: T)

data class Test3<T>(val x: List<T>)

data class Test4(val x: List<String>)
