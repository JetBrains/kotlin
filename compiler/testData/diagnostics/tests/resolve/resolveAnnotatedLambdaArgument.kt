// !DIAGNOSTICS: -UNUSED_PARAMETER
target(AnnotationTarget.EXPRESSION)
annotation(repeatable = true) class Ann

fun <T> bar(block: (T) -> Int) {}

fun foo() {
    bar<Int> @Ann @[Ann] { x -> x }
    bar<Int> @Ann @[Ann] label@{ x -> x }
}
