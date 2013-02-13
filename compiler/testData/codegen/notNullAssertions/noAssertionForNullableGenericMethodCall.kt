import java.util.HashMap

class A<T: Any> {
    fun main() {
        HashMap<String, T>()[""]
    }
}