// IS_APPLICABLE: false
fun main(args: Array<String>){
    if (foo == bar<caret>) {
        bar.foo()
    } else null
}
