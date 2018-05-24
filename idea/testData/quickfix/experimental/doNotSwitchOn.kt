// "Add '-Xuse-experimental=kotlin.Experimental' to module light_idea_test_case compiler arguments" "false"
// COMPILER_ARGUMENTS: -version -Xuse-experimental=Something
// DISABLE-ERRORS
// WITH_RUNTIME
// ACTION: Make internal
// ACTION: Make private

@Experimental<caret>
annotation class MyExperimentalAPI