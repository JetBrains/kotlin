fun call() {
    val ktClass = KtClass()
    ktClass.<expr>instance</expr>.foo = 42
}


class KtClass {
    val instance : KtSubClass = KtSubClass()
}

class KtSubClass {
    var foo : Int = -1
}
