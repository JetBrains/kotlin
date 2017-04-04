import kotlin.test.*

fun box() {
    expect(-1) { arrayOf("cat", "dog", "bird").indexOf("mouse") }
    expect(0) { arrayOf("cat", "dog", "bird").indexOf("cat") }
    expect(1) { arrayOf("cat", "dog", "bird").indexOf("dog") }
    expect(2) { arrayOf("cat", "dog", "bird").indexOf("bird") }
    expect(0) { arrayOf(null, "dog", null).indexOf(null as String?) }

    expect(-1) { arrayOf("cat", "dog", "bird").indexOfFirst { it.contains("p") } }
    expect(0) { arrayOf("cat", "dog", "bird").indexOfFirst { it.startsWith('c') } }
    expect(1) { arrayOf("cat", "dog", "bird").indexOfFirst { it.startsWith('d') } }
    expect(2) { arrayOf("cat", "dog", "bird").indexOfFirst { it.endsWith('d') } }

    expect(-1) { sequenceOf("cat", "dog", "bird").indexOfFirst { it.contains("p") } }
    expect(0) { sequenceOf("cat", "dog", "bird").indexOfFirst { it.startsWith('c') } }
    expect(1) { sequenceOf("cat", "dog", "bird").indexOfFirst { it.startsWith('d') } }
    expect(2) { sequenceOf("cat", "dog", "bird").indexOfFirst { it.endsWith('d') } }
}
