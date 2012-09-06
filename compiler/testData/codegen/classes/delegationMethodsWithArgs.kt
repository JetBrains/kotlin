package test

trait TextField {
    fun getText(): String
}

trait InputTextField : TextField {
    fun setText(text: String)
}

trait MooableTextField : InputTextField {
    fun moo(a: Int, b: Int, c: Int): Int
}

class SimpleTextField : MooableTextField {
    private var text = ""
    override fun getText() = text
    override fun setText(text: String) {
        this.text = text
    }
    override fun moo(a: Int, b: Int, c: Int) = a + b + c
}

class TextFieldWrapper(textField: MooableTextField) : MooableTextField by textField

fun box() : String {
    val textField = TextFieldWrapper(SimpleTextField())
    textField.setText("hello world!")

    if (!textField.getText().equals("hello world!")) return "FAIL #!1"
    if (textField.moo(1,2,3) != 6) return "FAIL #2"

    return "OK"
}