// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-57568

fun <T, K> T.convert(): K = null!!

fun of(size: ULong) {
    of(size.convert())
}

fun of(size: Int) {}

fun of(size: Long) {}

/* GENERATED_FIR_TAGS: checkNotNullCall, funWithExtensionReceiver, functionDeclaration, nullableType, typeParameter */
