// RUN_PIPELINE_TILL: FRONTEND
sealed class Tree {
    object Empty: Tree()
    class Leaf(val x: Int): Tree()
    class Node(val left: Tree, val right: Tree): Tree()

    fun max(): Int = <!WHEN_ON_SEALED!>when(this) {
        Empty -> -1
        is Leaf  -> this.x
        is Node  -> this.left.max()
    }<!>

    fun maxIsClass(): Int = <!NO_ELSE_IN_WHEN!>when<!>(this) {
        Empty -> -1
        <!NO_COMPANION_OBJECT!>Leaf<!>  -> 0
        is Node  -> this.left.max()
    }

    fun maxWithElse(): Int = <!WHEN_ON_SEALED!>when(this) {
        is Leaf  -> this.x
        is Node  -> this.left.max()
        else -> -1
    }<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, functionDeclaration, integerLiteral, isExpression,
nestedClass, objectDeclaration, primaryConstructor, propertyDeclaration, sealed, smartcast, thisExpression,
whenExpression, whenWithSubject */
