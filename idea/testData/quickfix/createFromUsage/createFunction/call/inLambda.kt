// "Create function 'foo'" "true"
// COMPILER_ARGUMENTS: -XXLanguage:-NewInference

fun <T> run(f: () -> T) = f()

fun test() {
    run { <caret>foo() }
}