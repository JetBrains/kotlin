// "Replace with '@' annotations" "true"

annotation class Ann(val x: Int = 1)

@Ann @Ann(2) class MyClass