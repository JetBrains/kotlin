// "Add 'operator' modifier" "true"
// TOOL: org.jetbrains.kotlin.idea.inspections.AddOperatorModifierInspection

actual class Foo {
    actual fun <caret>unaryMinus() {

    }
}