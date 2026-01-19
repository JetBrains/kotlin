// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80500
//LANGUAGE: +CollectionLiterals

fun <T> foo(x: T, y: (T) -> Unit): T = x

class MyList<out E> {
    companion object {
        operator fun <E> of(vararg x: E): MyList<E> = MyList()
    }
}

fun MyList<String>.impl() {
}

fun main() {
    val w: MyList<CharSequence> = foo([""]) { x ->
        x.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>impl<!>()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, funWithExtensionReceiver, functionDeclaration, functionalType,
lambdaLiteral, localProperty, nullableType, objectDeclaration, operator, out, propertyDeclaration, stringLiteral,
typeParameter, vararg */
