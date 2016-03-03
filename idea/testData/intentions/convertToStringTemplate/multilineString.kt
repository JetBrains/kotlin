// IS_APPLICABLE: false
fun main(args: Array<String>){
    var r = "a"
    val x = "foo" +<caret> """bar
    $r"""
}
