import kotlin.test.*

fun box() {
    expect(-1) { arrayOf("cat", "dog", "bird").lastIndexOf("mouse") }
    expect(0) { arrayOf("cat", "dog", "bird").lastIndexOf("cat") }
    expect(1) { arrayOf("cat", "dog", "bird").lastIndexOf("dog") }
    expect(2) { arrayOf(null, "dog", null).lastIndexOf(null as String?) }
    expect(3) { arrayOf("cat", "dog", "bird", "dog").lastIndexOf("dog") }

    expect(-1) { arrayOf("cat", "dog", "bird").indexOfLast { it.contains("p") } }
    expect(0) { arrayOf("cat", "dog", "bird").indexOfLast { it.startsWith('c') } }
    expect(2) { arrayOf("cat", "dog", "cap", "bird").indexOfLast { it.startsWith('c') } }
    expect(2) { arrayOf("cat", "dog", "bird").indexOfLast { it.endsWith('d') } }
    expect(3) { arrayOf("cat", "dog", "bird", "red").indexOfLast { it.endsWith('d') } }

    expect(-1) { sequenceOf("cat", "dog", "bird").indexOfLast { it.contains("p") } }
    expect(0) { sequenceOf("cat", "dog", "bird").indexOfLast { it.startsWith('c') } }
    expect(2) { sequenceOf("cat", "dog", "cap", "bird").indexOfLast { it.startsWith('c') } }
    expect(2) { sequenceOf("cat", "dog", "bird").indexOfLast { it.endsWith('d') } }
    expect(3) { sequenceOf("cat", "dog", "bird", "red").indexOfLast { it.endsWith('d') } }
}
