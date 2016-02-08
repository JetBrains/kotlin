// WITH_RUNTIME
// IS_APPLICABLE: false
fun main(args: Array<String>){
    val y = "abcd" +<caret> listOf( 1,
                             2 )
}
