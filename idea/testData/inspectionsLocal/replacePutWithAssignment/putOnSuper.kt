// PROBLEM: none
// RUNTIME_WITH_FULL_JDK

class Test : java.util.LinkedHashMap<String, String>() {
    fun test() {
        super.<caret>put("foo", "bar")
    }
}