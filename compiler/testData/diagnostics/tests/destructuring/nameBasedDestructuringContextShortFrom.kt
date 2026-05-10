// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// LANGUAGE: +NameBasedDestructuring, +EnableNameBasedDestructuringShortForm, +ContextParameters

class Tuple(val first: String, val second: Int)

context(instance: Tuple)
fun ctx() = instance

context(instance: Tuple)
val ctxProp: Tuple get() = instance

context(list: List<Tuple>)
fun ctxList() = list

fun declaration(instance: Tuple) {
    context(instance) {
        if (true) { val (first, second,) = ctx() }
        if (true) { val (isActive = first, number = second, ext = first) = ctx() }
        if (true) { val (pCtxCProp = first) = ctxProp }
    }
}

fun loop(list: List<Tuple>) {
    context(list) {
        for ((first, second) in ctxList()) {}
        for ((first) in ctxList()) {}
        for ((first: String) in ctxList()) {}
        for ((aa = first, bb = second) in ctxList()) {}
        for ((pCtxCProp = first) in ctxList()) {}
    }
}

fun lambda(instance: Tuple) {
    context(instance) {
        ctx().let { (first, second) -> }
        ctx().let { (first) -> }
        ctx().let { (first: String) -> }
        ctx().let { (aa = first, bb = second) -> }
        ctx().let { (pCtxCProp = first) -> }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, destructuringDeclaration, forLoop, functionDeclaration, functionalType, getter,
ifExpression, integerLiteral, lambdaLiteral, localFunction, localProperty, primaryConstructor, propertyDeclaration,
stringLiteral */
