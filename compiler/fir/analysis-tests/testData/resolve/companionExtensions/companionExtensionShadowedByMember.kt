// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +CompanionBlocksAndExtensions

class C {
    fun member() {}
    fun member2() {}

    companion {
        fun static() {}
        fun static2() {}
    }
}

companion fun C.member() {}
companion fun C.<!EXTENSION_SHADOWED_BY_MEMBER!>static<!>() {}

fun C.<!EXTENSION_SHADOWED_BY_MEMBER!>member2<!>() {}
fun C.static2() {}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration */
