// CHECK_BYTECODE_TEXT
// 2 invoke\(

// After KT-84115 no `invoke` should left

fun interface FunInterface1 {
    operator fun invoke(): String
}

fun interface FunInterface2 {
    fun sam(): String
}

fun box(): String {
    return FunInterface1 { "O" }() + FunInterface2 { "K" }.sam()
}