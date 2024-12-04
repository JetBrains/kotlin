interface Test {
    fun foo() = "abc"
    fun String.bar() = "def"
}

// 1 check
//  ^ no null check for dispatch receiver parameters in 'foo' and 'bar',
//    extension receiver parameter in 'bar' is checked
