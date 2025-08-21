// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-65050

interface HttpResponse<T>

class GitLabMergeRequestShortRestDTO

fun loadMergeRequests(): HttpResponse<out List<GitLabMergeRequestShortRestDTO>> = TODO()

inline fun <reified T> loadList(): HttpResponse<out List<T>> = TODO()

fun loadMergeRequests(boolean: Boolean) {
    val response = if (boolean) {
        loadMergeRequests()
    } else {
        loadList()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, ifExpression, inline, interfaceDeclaration, localProperty,
nullableType, outProjection, propertyDeclaration, reified, typeParameter */
