package test

@HiddenDeclaration
fun hiddenFun(){}

fun notHiddenFun(){}

@HiddenDeclaration
var hiddenProperty: Int = 1

var notHiddenProperty: Int = 1

@HiddenDeclaration
val String.hiddenExtension: Int get() = 1
