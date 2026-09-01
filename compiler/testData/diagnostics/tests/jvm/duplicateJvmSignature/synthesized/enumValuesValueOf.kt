// RUN_PIPELINE_TILL: BACKEND
enum class <!CONFLICTING_JVM_DECLARATIONS!>A<!> {
    A1,
    A2;

    <!CONFLICTING_JVM_DECLARATIONS!>fun valueOf(s: String): A<!> = valueOf(s)

    fun valueOf() = "OK"

    <!CONFLICTING_JVM_DECLARATIONS!>fun values(): Array<A><!> = null!!

    fun values(x: String) = x
}

/* GENERATED_FIR_TAGS: checkNotNullCall, enumDeclaration, enumEntry, functionDeclaration, stringLiteral */
