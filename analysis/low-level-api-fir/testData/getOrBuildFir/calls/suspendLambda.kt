// WITH_STDLIB
// LANGUAGE: +ParseLambdaWithSuspendModifier
// SKIP_CONTAINMENT_CHECK
// SKIP_WHEN_OUT_OF_CONTENT_ROOT

fun test() {
    <expr>suspend {}</expr>
}