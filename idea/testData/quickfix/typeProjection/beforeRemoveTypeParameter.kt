// "Change 'List<String>' to 'List<*>'" "true"
fun isStringList(list : Any) = list is (List<<caret>String>)