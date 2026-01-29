// TARGET_BACKEND: WASM

// see DIRECTIVES at start of functions

// The debugger view in chrome sorts by name (lexicographically by ascii to be precise).
// To sort away internal/synthetic local vars, they are renamed with a tilde ~ at the start.
// This test checks that certain locals are and others aren't renamed.

// WASM_CHECK_LOCAL_IN_FUNCTION: name=iShouldNotBeRenamed inFunction=dontRenameNormal
fun dontRenameNormal(): Int{
    val iShouldNotBeRenamed = 42
    return iShouldNotBeRenamed
}

// should be renamed to ~inductionVariable
// WASM_CHECK_LOCAL_IN_FUNCTION: name="~inductionVariable" inFunction=inductionVar
// WASM_CHECK_LOCAL_NOT_IN_FUNCTION: name=inductionVariable inFunction=inductionVar
fun inductionVar(): Int {
    var x = 0
    // x = (6*7/2)*2
    for (i in 0..6) // uses `inductionVariable` as a local internal variable
        x+=i
    x*=2
    return x
}

// shouldn't rename any of these
// WASM_CHECK_LOCAL_IN_FUNCTION: name=explicitLambdaParameter inFunction=lambdas
// WASM_CHECK_LOCAL_IN_FUNCTION: name=it inFunction=lambdas
// WASM_CHECK_LOCAL_IN_FUNCTION: name=explicitNested inFunction=lambdas
fun lambdas() : Int {
    val lam1 = {explicitLambdaParameter:Int ->
        explicitLambdaParameter*2+1
    }

    val lam2:(Int) -> Int = {//implicit it
        it*2+1
    }

    val lam3 = {
        val nestedLam = {explicitNested:Int ->
            0
        }
        nestedLam(42)
    }
    return lam1(10) + lam2(10) + lam3()
}

fun box(): String {
    if(dontRenameNormal() != 42
        || inductionVar() != 42
        || lambdas() != 42)
        return "NOT OK"

    return "OK"
}
