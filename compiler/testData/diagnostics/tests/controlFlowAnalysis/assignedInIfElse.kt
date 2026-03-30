// RUN_PIPELINE_TILL: BACKEND
fun foo(arg: Boolean) {
    val x : Int
    if (arg) {
        x = 4
    } else {
        x = 2
    }

    x.hashCode()

    class Local {
        fun bar() {
            x.hashCode()
        }
    }

    fun local(): Int {
        return x.hashCode()
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionDeclaration, ifExpression, integerLiteral, localClass,
localFunction, localProperty, propertyDeclaration */
