// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// LANGUAGE: +ContextReceivers, -ContextParameters
// FIR_IDENTICAL

class Button
class ClickEvent

typealias ClickHandler = context(Button) (ClickEvent) -> Unit

fun handleClick(clickHandler: ClickHandler) {
    clickHandler(Button(), ClickEvent())
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, typeAliasDeclaration, typeWithContext */
