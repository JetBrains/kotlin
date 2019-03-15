// FILE: 1.kt
// IGNORE_BACKEND: JVM_IR
package test
abstract class Introspector {
    abstract inner class SchemaRetriever(val transaction: String) {
        inline fun inSchema(crossinline modifier: (String) -> Unit) =
                { modifier.invoke(transaction) }()
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


// FILE: 1.smap

SMAP
1.kt
Kotlin
*S Kotlin
*F
+ 1 1.kt
test/Introspector$SchemaRetriever$inSchema$1
*L
1#1,11:1
*E

// FILE: 2.smap

SMAP
2.kt
Kotlin
*S Kotlin
*F
+ 1 2.kt
IntrospectorImpl$SchemaRetriever
+ 2 1.kt
test/Introspector$SchemaRetriever
*L
1#1,21:1
7#2:22
*E
*S KotlinDebug
*F
+ 1 2.kt
IntrospectorImpl$SchemaRetriever
*L
9#1:22
*E

SMAP
1.kt
Kotlin
*S Kotlin
*F
+ 1 1.kt
test/Introspector$SchemaRetriever$inSchema$1
+ 2 2.kt
IntrospectorImpl$SchemaRetriever
*L
1#1,11:1
9#2:12
*E