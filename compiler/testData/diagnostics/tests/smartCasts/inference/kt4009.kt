trait PsiElement {
    fun getText(): String
    fun getParent(): PsiElement
}

trait JetExpression : PsiElement

fun foo1(e: PsiElement) {
    var current: PsiElement? = e
    var first = true
    while (current != null) {
        if (current is JetExpression && first) {
            println(current!!.getText()) // error: smart cast not possible. But it's not needed in fact!
        }

        current = current?.getParent()
    }
}

//from library
fun println(any: Any?) = throw Exception("$any")