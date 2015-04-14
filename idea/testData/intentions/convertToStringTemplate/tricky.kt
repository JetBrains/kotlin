fun compute1(): Int = 777
fun main(args: Array<String>){
    val a = "a"
    "a = " + a +<caret> ", b = " + (compute1() + 222) + " :)"
}
