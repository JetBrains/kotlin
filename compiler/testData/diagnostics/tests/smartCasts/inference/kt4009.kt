interface PsiElement {
    fun getText(): String
    fun getParent(): PsiElement
}

interface JetExpression : PsiElement

fun foo1(e: PsiElement) {
    var current: PsiElement? = e
    var first = true
    while (current != null) {
        if (current is JetExpression && first) {
            // Smartcast is possible here
            println(<!DEBUG_INFO_SMARTCAST!>current<!>.getText())
        }

        current = <!DEBUG_INFO_SMARTCAST!>current<!>.getParent()
    }
}

//from library
fun println(any: Any?): Nothing = throw Exception("$any")