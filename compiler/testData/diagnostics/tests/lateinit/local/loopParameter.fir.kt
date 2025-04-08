// RUN_PIPELINE_TILL: BACKEND

fun test(l: List<String>) {
    for (lateinit x in l) {}
}
