// LANGUAGE: +CompanionBlocksAndExtensions
class MyClass

companion fun MyClass.func(s: String) = s
companion val MyClass.readonly = "O"
companion var MyClass.mutable = ""

companion fun MyClass.getOk(): String {
    mutable = "K"
    return readonly + mutable
}

class A {
    companion {
        val compBlockVal: String = ""
        var compBlockVar: String = ""
        fun compBlockFun(k: String = "") = ""
    }
}
