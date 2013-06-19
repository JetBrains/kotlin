// IS_APPLICABLE: false
fun foo() {
    return run(1, 2) <caret>{
        println("lambda")
        println("another lambda")
    }
}
