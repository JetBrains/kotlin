// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// LANGUAGE: +ContextReceivers
// FIR_IDENTICAL

class Button
class ClickEvent

typealias ClickHandler = context(Button) (ClickEvent) -> Unit

fun handleClick(clickHandler: ClickHandler) {
    clickHandler(Button(), ClickEvent())
}