// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'filterIsInstance<>().sum()'"
// INTENTION_TEXT_2: "Replace with 'asSequence().filterIsInstance<>().sum()'"
fun foo(list: List<Any>){
    var result = 0
    <caret>for (l in list)
        if (l is Int)
            result += l
}