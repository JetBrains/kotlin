// p.Wrapper
package p

class Wrapper {
    data class Equals(val code: G) {
        override fun equals(other: Any?): Boolean = true
    }

    data class HashCode(val code: G) {
        override fun hashCode() = 3
    }

    data class ToString(val code: G) {
        override fun toString() = "b"
    }
}

class G
