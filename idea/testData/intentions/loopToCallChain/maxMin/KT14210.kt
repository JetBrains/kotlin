// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'filter{}.max()'"
// INTENTION_TEXT_2: "Replace with 'asSequence().filter{}.max()'"
fun f(list: List<Int>) {
    var result = -1
    <caret>for (item in list)
        if (item % 2 == 0)
            if (result <= item)
                result = item
}