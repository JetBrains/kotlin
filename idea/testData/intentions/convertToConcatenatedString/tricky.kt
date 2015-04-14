fun compute1(): Int = 777
fun main(args: Array<String>){
    val a = "a"
    "<caret>a = $a, b = ${compute1() + 222} :)"
}
