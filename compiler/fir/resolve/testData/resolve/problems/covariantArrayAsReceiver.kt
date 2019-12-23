interface UsageInfo {
    val usage: PsiElement
}

interface PsiElement

interface KtParameter : PsiElement

interface KtLightMethod : PsiElement

// With covariant array here we do not visit lambda (it.usage is KtLightMethod) below
// Problem disappears if 'out' is removed
fun <T> Array<out T>.filterNot(f: (T) -> Boolean): List<T> {
    return this
}

fun <T> Array<T>.toList(): List<T>? = null

fun foo(element: PsiElement, usages: Array<UsageInfo>) {
    val adjusted = if (element is KtParameter) usages.filterNot {
        it.usage is KtLightMethod
    } else usages.toList()
}