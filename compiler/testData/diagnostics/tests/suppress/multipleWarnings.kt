// RUN_PIPELINE_TILL: CODEGEN
// WITH_EXTRA_CHECKERS

@Suppress("REDUNDANT_VISIBILITY_MODIFIER")
public class A {
    @Suppress("REDUNDANT_MODALITY_MODIFIER")
    public final fun foo() {}
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, stringLiteral */
