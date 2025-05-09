interface Introspector {

    class SchemaRetriever(val transaction: String) {
        inline fun inSchema(crossinline modifier: (String) -> Unit) =
                { modifier(transaction) }.let { it() }

        internal fun retrieve() {
            inSchema { schema -> "OK" }
        }
    }

}

// TESTED_OBJECT_KIND: innerClass
// TESTED_OBJECTS: Introspector$SchemaRetriever$inSchema$1$kotlin_Function0$0, SchemaRetriever
// FLAGS: ACC_FINAL, ACC_PUBLIC, ACC_STATIC

// TESTED_OBJECT_KIND: innerClass
// TESTED_OBJECTS: Introspector$SchemaRetriever$inSchema$1$kotlin_Function0$0, DefaultImpls
// ABSENT: true
