class Declaration {
    fun lambdaType(p: Int, f: (Int) -> Int) = f(p)
}

fun call(declaration: Declaration) {
    declaration.lambdaType(10) {<caret> _ -> 11 }
}