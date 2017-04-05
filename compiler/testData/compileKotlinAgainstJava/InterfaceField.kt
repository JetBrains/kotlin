package test

fun useField() = InterfaceField.STRING

fun implementInterface() = object : InterfaceField {
    override fun func() = "str"
}

fun useFunc() = implementInterface().func()
