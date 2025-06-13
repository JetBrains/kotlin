// RUN_PIPELINE_TILL: BACKEND
interface A {
    fun foo()
}

interface B {
    fun bar()
}

fun B.b() {
    object : A {
        override fun foo() {
            this@b.bar()
        }
    }
}


fun test() {
    fun <T> without(f: T.() -> Unit): Unit = (null!!).f()
    without<B>() b@ {
        object : A {
            override fun foo() {
                this@b.bar()
            }
        }
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, checkNotNullCall, funWithExtensionReceiver, functionDeclaration,
functionalType, interfaceDeclaration, lambdaLiteral, localFunction, nullableType, override, thisExpression,
typeParameter, typeWithExtension */
