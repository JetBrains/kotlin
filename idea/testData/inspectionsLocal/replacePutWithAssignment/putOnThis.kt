// WITH_RUNTIME

class MyMap() : HashMap<String, String>() {
    init {
        this.put<caret>("foo", "bar")
    }
}