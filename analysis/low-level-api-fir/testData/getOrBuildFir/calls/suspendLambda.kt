// WITH_STDLIB
// LANGUAGE: +ParseLambdaWithSuspendModifier
// SKIP_CONTAINMENT_CHECK

fun test() {
    <expr>suspend {}</expr>
}