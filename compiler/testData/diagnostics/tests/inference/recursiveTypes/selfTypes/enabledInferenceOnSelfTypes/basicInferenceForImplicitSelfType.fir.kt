// !LANGUAGE: +TypeInferenceOnCallsWithSelfTypes

// FILE: JavaBuilder.java
public class JavaBuilder<B extends JavaBuilder<B>> {
    <T extends B> T test() {
        return null;
    }
    void foo() { }
}

// FILE: main.kt
class Builder<B : Builder<B>> {
    fun <T : B> test(): T = TODO()

    fun foo() {}
}

fun testStar(builder: Builder<*>) {
    <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Cannot infer argument for type parameter T")!>builder.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>test<!>()<!>

    builder
        .<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>test<!>()
        .<!UNRESOLVED_REFERENCE!>foo<!>()
}

fun <K : Builder<K>> testTypeParam(builder: Builder<K>) {
    <!DEBUG_INFO_EXPRESSION_TYPE("K")!>builder.test()<!>

    builder
        .test()
        .foo()
}

fun testStarJava(builder: JavaBuilder<*>) {
    <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Cannot infer argument for type parameter T")!>builder.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>test<!>()<!>

    builder
        .<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>test<!>()
        .<!UNRESOLVED_REFERENCE!>foo<!>()
}

fun <K : JavaBuilder<K>> testTypeParamJava(builder: JavaBuilder<K>) {
    <!DEBUG_INFO_EXPRESSION_TYPE("K..K?!")!>builder.test()<!>

    builder
        .test()
        .foo()
}
