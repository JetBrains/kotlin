// IGNORE_INLINER: IR

// FILE: 1.kt


package test
abstract class Introspector {
    abstract inner class SchemaRetriever(val transaction: String) {
        inline fun inSchema(crossinline modifier: (String) -> Unit)
          { val lambda = { modifier.invoke(transaction) }; lambda() }
    }
}

// FILE: 2.kt
import test.*

var result = "fail"

class IntrospectorImpl() : Introspector() {
    inner class SchemaRetriever(transaction: String) : Introspector.SchemaRetriever(transaction) {
        internal fun retrieve() {
            inSchema { schema -> result = schema }
        }
    }
}

fun box(): String {
    IntrospectorImpl().SchemaRetriever("OK").retrieve()

    return result
}
