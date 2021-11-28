// !LANGUAGE: +TypeInferenceOnCallsWithSelfTypes

// FILE: JavaBodySpec.java
public interface JavaBodySpec<B, S extends JavaBodySpec<B, S>> {
    default <T extends S> T isEqualTo(B expected) {
        return null;
    }
}

// FILE: main.kt
interface BodySpec<B, S : BodySpec<B, S>> {
    fun <T : S> isEqualTo(expected: B): T
}

fun test(b: BodySpec<String, *>) {
    val x = b.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>isEqualTo<!>("")
    <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Cannot infer argument for type parameter T")!>x<!>
}

fun testJava(b: JavaBodySpec<String, *>) {
    val x = b.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>isEqualTo<!>("")
    <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Cannot infer argument for type parameter T")!>x<!>
}
