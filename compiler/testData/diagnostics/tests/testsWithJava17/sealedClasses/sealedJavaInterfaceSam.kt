// RUN_PIPELINE_TILL: BACKEND
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
    acceptFace(IFace {})
    acceptFace({})
}

/* GENERATED_FIR_TAGS: functionDeclaration, lambdaLiteral */
