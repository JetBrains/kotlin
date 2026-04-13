// RUN_PIPELINE_TILL: FRONTEND
fun box(): String {
    KlassA({})
    KlassB()
    return "OK"
}

class KlassA(arg: (() -> Unit)?) {
    var func: (() -> Unit)? = arg
    init {
        if (func != null) {
            <!SMARTCAST_IMPOSSIBLE_ON_IMPLICIT_INVOKE_RECEIVER!>func<!>()
        }
    }
}

class KlassB {
    var func: (() -> Unit)?
    init {
        if (true) {
            func = {}
            <!UNSAFE_IMPLICIT_INVOKE_CALL!>func<!>()
        } else {
            func = null
        }
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, equalityExpression, functionDeclaration, functionalType,
ifExpression, init, lambdaLiteral, nullableType, primaryConstructor, propertyDeclaration, smartcast, stringLiteral */
