// IS_APPLICABLE: false
fun main(args: Array<String>){
    if (foo != null<caret>){
        print ("Hello")
            foo.bar()
    }
}
