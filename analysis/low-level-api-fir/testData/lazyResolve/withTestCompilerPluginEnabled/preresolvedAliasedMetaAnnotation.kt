// WITH_FIR_TEST_COMPILER_PLUGIN
// SKIP_WHEN_OUT_OF_CONTENT_ROOT

typealias MyTypeAlias = org.jetbrains.kotlin.plugin.sandbox.MetaSupertype

@MyTypeAlias
annotation cla<caret_preresolved>ss MyAnno

@MyAnno
clas<caret>s A
