public open class Z {
    fun zzz() {

    }
}

// NEXT_SIBLING:
public class A(): Z() {
    var a: Int = 1

    fun foo(): Int {
        <selection>super.zzz()
        return a + 1</selection>
    }
}
