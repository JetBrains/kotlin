import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    expect(listOf(1, 2, 3, 4, 5)) { (1..10).take(5) }
    expect(listOf(1, 2, 3, 4, 5)) { (1..10).toList().take(5) }
    expect(listOf(1, 2)) { (1..10).take(2) }
    expect(listOf(1, 2)) { (1..10).toList().take(2) }
    expect(true) { (0L..5L).take(0).none() }
    expect(true) { listOf(1L).take(0).none() }
    expect(listOf(1)) { (1..1).take(10) }
    expect(listOf(1)) { listOf(1).take(10) }
}
