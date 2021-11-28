@kotlin.ExperimentalMultiplatform
@kotlin.OptionalExpectation
expect annotation class Ann()

@Ann
@kotlin.ExperimentalMultiplatform
fun foo() {}
