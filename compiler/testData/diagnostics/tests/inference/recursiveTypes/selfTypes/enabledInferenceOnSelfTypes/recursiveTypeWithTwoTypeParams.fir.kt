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
    val x = b.isEqualTo("")
    <!DEBUG_INFO_EXPRESSION_TYPE("BodySpec<kotlin.String, *>")!>x<!>
}

fun testJava(b: JavaBodySpec<String, *>) {
    val x = b.isEqualTo("")
    <!DEBUG_INFO_EXPRESSION_TYPE("JavaBodySpec<kotlin.String..kotlin.String?!, *>..JavaBodySpec<kotlin.String..kotlin.String?!, *>?!")!>x<!>
}
