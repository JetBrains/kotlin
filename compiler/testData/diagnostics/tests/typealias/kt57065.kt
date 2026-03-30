// RUN_PIPELINE_TILL: BACKEND

typealias GlobalUndoLogRef = Long
fun GlobalUndoLogRef(p: Long): GlobalUndoLogRef = p

fun main() {
    GlobalUndoLogRef(42)
}

/* GENERATED_FIR_TAGS: functionDeclaration, typeAliasDeclaration */
