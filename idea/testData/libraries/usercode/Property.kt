import testData.libraries.*

fun foo(a : ClassWithAbstractAndOpenMembers) {
    a.abstractVar = "v"
    println(a.abstractVar)
}

// main.kt
//public abstract class <1>ClassWithAbstractAndOpenMembers {
//    public abstract fun abstractFun()
//    public open fun openFun() {
//    }
//
//    public abstract val abstractVal : String
//    public open val openVal : String = ""
//    public open val openValWithGetter : String
//    get() {
//        return "239"
//    }
//
//    public abstract var <2><3>abstractVar : String

