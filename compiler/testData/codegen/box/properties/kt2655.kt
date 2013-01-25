trait TextField {
    fun getText(): String
    fun setText(text: String)
}

class SimpleTextField : TextField {
    private var text = ""
    override fun getText() = text
    override fun setText(text: String) {
        this.text = text
    }
}

class TextFieldWrapper(textField: TextField) : TextField by textField

fun box() : String {
    val textField = TextFieldWrapper(SimpleTextField())
    textField.setText("OK")
    return textField.getText()
}
