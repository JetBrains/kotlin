// TARGET_PLATFORM: Common

public class A<caret>A(
    private val content: Map<String, Int>
) : Map<String, Int> by content {
    override fun toString(): String {
        return content.entries.toString()
    }
}