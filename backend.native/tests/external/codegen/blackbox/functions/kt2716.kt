package someTest

public class Some private constructor(val v: String) {
    companion object {
        public fun init(v: String): Some {
            return Some(v)
        }
    }
}

fun box() = Some.init("OK").v
