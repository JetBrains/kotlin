public open class Z {
    val z: Int = 0
}

// NEXT_SIBLING:
public class A(): Z() {
    var a: Int = 1

    public inner class B(): Z() {
        var b: Int = 1

        fun foo(): Int {
            return <selection>a + b + this@A.z + this@B.z</selection>
        }
    }
}
