// RUN_PIPELINE_TILL: FRONTEND
// Issue: KT-37736

internal class Z<K> {
    val map = HashMap<String, String>()
    inline fun compute(key: String, producer: () -> String): String {
        return map.getOrPut(key, ::<!UNSUPPORTED!>producer<!>)
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, inline, nullableType, propertyDeclaration,
typeParameter */
