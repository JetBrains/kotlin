// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_STDLIB
@JvmInline
value class WatchKind(val value: Int) {
    infix fun or(flag: WatchKind): WatchKind = WatchKind(value or flag.value)

    fun has(flag: WatchKind): Boolean = value and flag.value == flag.value

    companion object {
        val Create: WatchKind = WatchKind(0x01)
        val Change: WatchKind = WatchKind(0x02)
        val Delete: WatchKind = WatchKind(0x04)

        val All: WatchKind = Create or Change or Delete
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, equalityExpression, functionDeclaration, infix, integerLiteral,
objectDeclaration, primaryConstructor, propertyDeclaration, value */
