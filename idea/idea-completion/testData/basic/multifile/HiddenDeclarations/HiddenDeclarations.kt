package test

@HiddenDeclaration
fun hiddenFunFromSameFile(){}

fun String.foo() {
    hid<caret>
}

// INVOCATION_COUNT: 2
// ABSENT: hiddenFun
// ABSENT: hiddenProperty
// ABSENT: hiddenFunFromSameFile
// ABSENT: hiddenExtension
// EXIST: notHiddenFun
// EXIST: notHiddenProperty
