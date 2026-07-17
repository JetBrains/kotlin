// RUN_PIPELINE_TILL: FRONTEND
// FILE: IFace.java
public sealed interface IFace {
  void m();
}

final class JImpl implements IFace {
    @Override
    public void m() { }
}

// FILE: test.kt
fun acceptFace(i: IFace) {}

fun test() {
    acceptFace(<!INTERFACE_AS_FUNCTION!>IFace<!> {})
    acceptFace(<!ARGUMENT_TYPE_MISMATCH!>{}<!>)
}

/* GENERATED_FIR_TAGS: functionDeclaration, lambdaLiteral */
