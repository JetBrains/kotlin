import java.util.stream.Stream
import kotlin.streams.*

// kotlin-stdlib-jdk7

fun testUse(c: AutoCloseable) = c.use {}

// kotlin-stdlib-jdk8

fun testGet(c: MatchGroupCollection) = c.get("")

fun testGetOrDefault(c: Map<out Number, Any>) = c.getOrDefault(42, "")
fun testRemove(c: MutableMap<out Number, Any>) = c.remove(42, "")

fun testAsSequence(c: Stream<String>) = c.asSequence()
