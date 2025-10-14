// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// LANGUAGE: +NameBasedDestructuring, +ContextParameters

class Tuple(val first: String, val second: Int)

context(instance: Tuple)
fun ctx() = instance

context(instance: Tuple)
val ctxProp: Tuple get() = instance

context(list: List<Tuple>)
fun ctxList() = list

fun declaration(instance: Tuple) {
    context(instance) {
        if (true) { ( val first, val second,) = ctx() }
        if (true) { (val isActive = first, val number = second, val ext = first) = ctx() }
        if (true) { (val pCtxCProp = first) = ctxProp }
    }
}

fun loop(list: List<Tuple>) {
    context(list) {
        for ((val first, val second) in ctxList()) {}
        for ((val first) in ctxList()) {}
        for ((val first: String) in ctxList()) {}
        for ((val aa = first, val bb = second) in ctxList()) {}
        for ((val pCtxCProp = first) in ctxList()) {}
    }
}

fun lambda(instance: Tuple) {
    context(instance) {
        ctx().let { (val first, val second) -> }
        ctx().let { (val first) -> }
        ctx().let { (val first: String) -> }
        ctx().let { (val aa = first, val bb = second) -> }
        ctx().let { (val pCtxCProp = first) -> }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, destructuringDeclaration, forLoop, functionDeclaration, functionalType, getter,
ifExpression, integerLiteral, lambdaLiteral, localFunction, localProperty, primaryConstructor, propertyDeclaration,
stringLiteral */
