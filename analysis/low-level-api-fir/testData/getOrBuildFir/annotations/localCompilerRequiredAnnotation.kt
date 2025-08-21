// LOOK_UP_FOR_ELEMENT_OF_TYPE: org.jetbrains.kotlin.psi.KtAnnotationEntry
// WITH_FIR_TEST_COMPILER_PLUGIN
// SKIP_WHEN_OUT_OF_CONTENT_ROOT

@org.jetbrains.kotlin.plugin.sandbox.MetaSupertype
@Target(AnnotationTarget.CLASS)
annotation class MyAnno(val value: String)

fun usage() {
    <expr>@MyAnno("str")</expr>
    class LocalClass {

    }
}
