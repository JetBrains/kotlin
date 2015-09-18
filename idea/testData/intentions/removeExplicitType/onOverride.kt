public interface I {
    public fun foo(): String
}

public class C : I {
    override fun foo(): <caret>String = ""
}