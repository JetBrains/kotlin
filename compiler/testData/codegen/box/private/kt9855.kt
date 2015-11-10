class MyString(var content : String)

object Greeter {
    fun sayHello(name : String): String {
        var result = MyString(name)
        result += "K"
        return result.content
    }
}

private operator fun MyString.plus(suffix: String) : MyString =  MyString("${this.content}$suffix")

fun box(): String {
    return Greeter.sayHello("O")
}