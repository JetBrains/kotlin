// "Add arrayOf wrapper" "true"

annotation class ArrAnn(val value: Array<String>)

@ArrAnn(<caret>"123") class My
