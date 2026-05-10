// LANGUAGE: +CompanionBlocksAndExtensions
// DUMP_KLIB_ABI: DEFAULT
class C
class C2 {
    companion object
}

companion operator fun C.invoke(s: String) = s
companion operator fun C2.invoke(x: Any) = x

fun box(): String {
    return C("O") + C2("K")
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, objectDeclaration, operator,
stringLiteral */
