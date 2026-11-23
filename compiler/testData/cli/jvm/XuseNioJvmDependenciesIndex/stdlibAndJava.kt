import java.util.stream.Stream

fun test(): List<Int> {
    JavaClass().run()
    val inlined = listOf(1, 2, 3).map { it * 2 }.filter { it > 2 }
    return inlined + Stream.of(42).toList() + JavaClass.answer()
}
