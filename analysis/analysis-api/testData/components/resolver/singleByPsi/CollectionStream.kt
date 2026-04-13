// ISSUE: KT-64044
// FULL_JDK
// RENDER_PSI_CLASS_NAME

fun f(collection: Collection<String>) {
    collection.strea<caret>m()
}
