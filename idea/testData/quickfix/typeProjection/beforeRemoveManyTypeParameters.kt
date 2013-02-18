// "Change 'Map<String, Int>' to 'Map<*, *>'" "true"
fun isStringToIntMap(map : Any) = map is (Map<<caret>String, Int>)