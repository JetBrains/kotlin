// MODULE: lib
// FILE: A.kt
@JvmRecord
data class MyRecord(val foo: String, val bar: String)

// MODULE: main(lib)
// FILE: B.kt

fun box(): String {
    val myRecord = MyRecord("O", "K")
    return myRecord.foo + myRecord.bar
}
