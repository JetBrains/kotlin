fun x() {
    <selection>foo(1)</selection>
}

fun foo(){}

// CALL: KtFunctionCall: targetFunction = ERR<Too many arguments for org.jetbrains.kotlin.fir.declarations.impl.FirSimpleFunctionImpl@5f1f1786: public final fun /foo(): R|kotlin/Unit| {
}
, [/foo(): kotlin.Unit]>