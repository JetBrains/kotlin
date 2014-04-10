public open class Z {
    val z: Int = 0
}

// NEXT_SIBLING:
public class A(): Z() {
    var a: Int = 1

    fun foo(): Int {
        <selection>return a + super.z</selection>
    }
}
