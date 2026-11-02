// RUN_PIPELINE_TILL: FRONTEND
annotation class Anno

fun annotatedIncompleteType(vararg p: @Anno Int.) {}
fun annotatedIncompleteUnresolvedType(vararg p: @Anno Unresolved.) {}

class Constructor(vararg p: @Anno Int.)
class ConstructorProperty(vararg val p: @Anno Int.)

fun localDeclarations() {
    fun local(vararg p: @Anno Int.) {}
    val anonymous = fun(vararg p: @Anno Int.) {}
}

fun nonVarargAnnotatedIncompleteType(p: @Anno Int.) {}
