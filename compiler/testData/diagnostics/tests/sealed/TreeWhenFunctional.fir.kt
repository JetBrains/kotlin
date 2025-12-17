// RUN_PIPELINE_TILL: BACKEND
sealed class Tree {
    object Empty: Tree()
    class Leaf(val x: Int): Tree()
    class Node(val left: Tree, val right: Tree): Tree()

    fun max(): Int = <!WHEN_ON_SEALED_GEEN_ELSE!>when(this) {
        is Empty -> -1
        is Leaf  -> this.x
        is Node  -> this.left.max()
    }<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, isExpression, nestedClass,
objectDeclaration, primaryConstructor, propertyDeclaration, sealed, smartcast, thisExpression, whenExpression,
whenWithSubject */
