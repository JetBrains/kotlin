
class A{
    fun printName(name: String?){
        name!!<caret>.length
    }
}