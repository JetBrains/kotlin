import java.util.stream.*

interface A : MutableCollection<String> {
    override fun removeIf(x: java.util.function.Predicate<in String>) = false
}

fun foo(x: MutableList<String>, y: A) {
    x.removeIf { it.length > 0 }
    y.removeIf { it.length > 0 }
}
