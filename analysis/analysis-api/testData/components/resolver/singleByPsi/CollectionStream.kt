// ISSUE: KT-64044
// IGNORE_FE10
// (stream call is unresolved for some reason)
// FULL_JDK
// RENDER_PSI_CLASS_NAME

fun f(collection: Collection<String>) {
    collection.strea<caret>m()
}
