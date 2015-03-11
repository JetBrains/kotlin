package someTest

public class Some private(val v: String) {
    default object {
        public fun init(v: String): Some {
            return Some(v)
        }
    }
}

fun box() = Some.init("OK").v
