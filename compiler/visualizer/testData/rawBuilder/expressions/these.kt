// FIR_IGNORE
class Some {
//                 Int
//                 │ Int
//                 │ │
    fun foo(): Int = 1

    fun bar(): Int {
//                  fun (Some).foo(): Int
//                  │
        return this.foo()
    }

//      Some
//      │
    val instance: Some
        get() = this@Some

    fun String.extension(): Int {
//                       fun (Some).bar(): Int
//                       │     fun (Int).plus(Int): Int
//                       │     │      val (String).length: Int
//                       │     │      │
        return this@Some.bar() + this.length
    }
}

//                   Int    fun (Some).bar(): Int
//                   │      │
fun Some.extension() = this.bar()

fun test(some: Some): Int {
//         fun <T, R> with<Some, Int>(T, T.() -> R): R
//         │    test.some: Some
//         │    │     with@0
//         │    │     │
    return with(some) {
//           fun (Some).foo(): Int
//           │     fun (Int).plus(Int): Int
//           │     │           fun Some.extension(): Int
//           │     │           │
        this.foo() + this@with.extension()
    }
}
