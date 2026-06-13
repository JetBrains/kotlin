// LANGUAGE: +CompanionBlocksAndExtensions
// DUMP_KLIB_ABI: DEFAULT
class C {
    companion {
        operator fun invoke(s: String) = s
    }
}
class C2 {
    companion {
        operator fun invoke(s: String) = s
    }

    companion object
}

fun box(): String {
    return C("O") + C2("K")
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, objectDeclaration, operator,
stringLiteral */
