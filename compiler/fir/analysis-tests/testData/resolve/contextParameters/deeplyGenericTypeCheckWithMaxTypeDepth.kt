// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters -DisableMaxTypeDepthFromInitialConstraints
// ISSUE: KT-81441
interface K<F, A>
interface Functor<F>

context(_: Functor<F>)
fun <F, A, B> K<F, A>.fmap(f: (A) -> B): K<F, B> = TODO()

context(_: Functor<Int>, _: Functor<String>)
fun <F, G> fails() {
    class Foo : Functor<K<K<List<*>, Char>, Boolean>> {
        fun K<Int, K<String, Int>>.bar(f: (Int) -> String) {
            <!TYPE_MISMATCH!>fmap { it.fmap(f) }<!>
        }
    }
}

context(_: Functor<Int>, _: Functor<String>)
fun <F, G> works() {
    with(TODO() as Functor<K<K<List<Int>, Char>, Boolean>>) {
        fun K<Int, K<String, Int>>.bar(f: (Int) -> String) {
            fmap { it.fmap(f) }
        }
    }
}

context(_: Functor<Int>, _: Functor<String>)
fun <F, G> works2() {
    class Foo : Functor<K<K<Int, Char>, Boolean>> {
        fun K<Int, K<String, Int>>.bar(f: (Int) -> String) {
            fmap { it.fmap(f) }
        }
    }
}

context(_: Functor<Int>, _: Functor<String>)
fun <F, G> works3() {
    class Foo : Functor<K<K<List<*>, Char>, Boolean>> {
        fun K<Int, K<String, Int>>.bar(f: (Int) -> String) {
            fmap {
                val foo = it.fmap(f)
                foo
            }
        }
    }
}

context(_: Functor<Int>, _: Functor<String>)
fun <F, G> works4() {
    class Foo : Functor<K<K<List<*>, Char>, Boolean>> {
        fun K<Int, K<String, Int>>.bar(f: (Int) -> String) {
            fmap { it.fmap<_, _, String>(f) }
        }
    }
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, funWithExtensionReceiver, functionDeclaration,
functionDeclarationWithContext, functionalType, interfaceDeclaration, lambdaLiteral, localClass, localFunction,
localProperty, nullableType, propertyDeclaration, starProjection, typeParameter */
