// "Add '-Xuse-experimental=kotlin.Experimental' to module light_idea_test_case compiler arguments" "true"
// COMPILER_ARGUMENTS: -version
// COMPILER_ARGUMENTS_AFTER: -version -Xuse-experimental=kotlin.Experimental
// DISABLE-ERRORS
// WITH_RUNTIME

@Experimental<caret>
annotation class MyExperimentalAPI