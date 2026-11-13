// IGNORE_STANDALONE
// `LLStandaloneFirElementByPsiElementChooser` crashes on building FIR type references with `createEmptySession()`
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtFunction

@file:JsModule("extModule")
package ext.jspackage.name
external fun foo()
