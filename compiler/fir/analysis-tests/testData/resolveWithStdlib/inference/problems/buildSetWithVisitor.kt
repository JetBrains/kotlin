// RUN_PIPELINE_TILL: BACKEND
abstract class AnyVisitor {
    abstract fun visit(arg: Wrapper)
}

class Wrapper(val tag: String)

fun Wrapper.accept(visitor: AnyVisitor) {
    visitor.visit(this)
}

fun bar(wrapper: Wrapper) = buildSet {
    wrapper.accept(object : AnyVisitor() {
        override fun visit(arg: Wrapper) {
            add(arg.tag)
        }
    })
}

fun foo(wrapper: Wrapper) = buildSet {
    wrapper.let {
        add(it.tag)
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, funWithExtensionReceiver, functionDeclaration,
lambdaLiteral, override, primaryConstructor, propertyDeclaration, thisExpression */
