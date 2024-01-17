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