interface Introspector {
    fun test() {
        class SchemaRetriever(val transaction: String) {
            inline fun inSchema(crossinline modifier: (String) -> Unit) =
                    { modifier(transaction) }()

            internal fun retrieve() {
                inSchema { schema -> "OK" }
            }
        }
    }
}

// TESTED_OBJECT_KIND: innerClass
// TESTED_OBJECTS: Introspector$test$SchemaRetriever, SchemaRetriever
// FLAGS: ACC_FINAL, ACC_PUBLIC, ACC_STATIC

// TESTED_OBJECT_KIND: innerClass
// TESTED_OBJECTS: Introspector$test$SchemaRetriever, DefaultImpls
// FLAGS: ACC_FINAL, ACC_PUBLIC, ACC_STATIC