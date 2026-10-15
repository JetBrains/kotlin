package research.kapt.gen

internal enum class Shape { DATA_CLASS, ANNOTATED_MEMBERS, GENERIC_CLASS, NESTED_TYPE, INHERITANCE }

internal fun shapeFor(index: Int): Shape = Shape.entries[index % Shape.entries.size]

internal fun renderShape(name: String, shape: Shape, mode: Mode): String {
    val classAnno = if (mode == Mode.SYNTHETIC) "@SyntheticAnno\n" else ""
    val ctorAnno = if (mode == Mode.DAGGER) "@Inject constructor" else "constructor"
    return when (shape) {
        Shape.DATA_CLASS ->
            "${classAnno}data class ${name}_Data $ctorAnno(val a: Int, val b: String, val c: Long)\n"
        Shape.ANNOTATED_MEMBERS -> {
            val memberAnno = if (mode == Mode.SYNTHETIC) "@field:SyntheticAnno " else ""
            """
            |${classAnno}class ${name}_Members $ctorAnno() {
            |    ${memberAnno}var x: Int = 0
            |    ${memberAnno}var y: String = ""
            |}
            |""".trimMargin()
        }
        Shape.GENERIC_CLASS ->
            "${classAnno}class ${name}_Generic<T : Comparable<T>, U : Number> $ctorAnno(val t: T, val u: U)\n"
        Shape.NESTED_TYPE ->
            """
            |${classAnno}class ${name}_Nested $ctorAnno(val v: Int) {
            |    class Inner(val w: String)
            |    inner class InnerRef(val parent: ${name}_Nested)
            |}
            |""".trimMargin()
        Shape.INHERITANCE ->
            """
            |${classAnno}open class ${name}_Base $ctorAnno(open val id: Int)
            |${classAnno}class ${name}_Derived $ctorAnno(override val id: Int, val extra: String) : ${name}_Base(id)
            |""".trimMargin()
    }
}

/**
 * A deliberately wide class: [memberCount] properties in one declaration.
 *
 * Not part of the [Shape] rotation — it is opt-in via the generator's `--wide` argument, so the
 * default scenarios keep producing byte-identical sources. Its purpose is to exercise kapt's
 * per-class javac list construction (class members, annotations) at a list length where an
 * O(n^2) accumulator is distinguishable from an O(n) one.
 */
internal fun renderWideClass(name: String, mode: Mode, memberCount: Int): String {
    val classAnno = if (mode == Mode.SYNTHETIC) "@SyntheticAnno\n" else ""
    val ctorAnno = if (mode == Mode.DAGGER) "@Inject constructor" else "constructor"
    val memberAnno = if (mode == Mode.SYNTHETIC) "@field:SyntheticAnno " else ""
    return buildString {
        append(classAnno)
        append("class ${name}_Wide $ctorAnno() {\n")
        for (i in 0 until memberCount) {
            append("    ${memberAnno}var p$i: Int = 0\n")
        }
        append("}\n")
    }
}

/**
 * A nesting-heavy class: an outer class with [nestingCount] nested classes and one property per
 * nested class, so every property type resolves through an internal name containing `$`.
 *
 * Not part of the [Shape] rotation — opt-in via the generator's `--nested` argument. Its purpose is
 * to exercise kapt's `KaptTreeMaker.getQualifiedName`, which only takes its expensive path for
 * `$`-containing names: the module-local branch (the nested classes below, which live in
 * `compiledClasses`) and the classpath/PSI branch (the `java.util.Map.Entry` properties).
 */
internal fun renderNestedClass(name: String, mode: Mode, nestingCount: Int): String {
    val classAnno = if (mode == Mode.SYNTHETIC) "@SyntheticAnno\n" else ""
    val ctorAnno = if (mode == Mode.DAGGER) "@Inject constructor" else "constructor"
    val memberAnno = if (mode == Mode.SYNTHETIC) "@field:SyntheticAnno " else ""
    return buildString {
        append(classAnno)
        append("class ${name}_Nesting $ctorAnno() {\n")
        for (i in 0 until nestingCount) {
            append("    class Inner$i(val v: Int)\n")
        }
        for (i in 0 until nestingCount) {
            append("    ${memberAnno}var n$i: Inner$i? = null\n")
        }
        // Classpath/PSI branch: a nested type that is not in this module.
        append("    ${memberAnno}var e0: java.util.Map.Entry<String, Int>? = null\n")
        append("    ${memberAnno}var e1: java.util.Map.Entry<Int, String>? = null\n")
        append("}\n")
    }
}

/**
 * A body-heavy class: [methodCount] methods with realistic statement counts, control flow, lambdas,
 * generic calls and a smart cast — the things `BODY_RESOLVE` actually has to work on.
 *
 * Not part of the [Shape] rotation — opt-in via the generator's `--bodies` argument. Every other
 * shape in this file is a signature holder with essentially empty bodies, which is why the existing
 * cells measure `BODY_RESOLVE` at 1.1-2.5% of Java-level work and cannot size a step that proposes
 * skipping it. The bodies below are deliberately inference-heavy (chained stdlib calls with lambda
 * parameters, `fold` with an accumulator, `associate` over entries) rather than merely long: phase
 * cost is driven by call resolution and type inference, not statement count.
 */
internal fun renderBodyHeavyClass(name: String, mode: Mode, methodCount: Int): String {
    val classAnno = if (mode == Mode.SYNTHETIC) "@SyntheticAnno\n" else ""
    val ctorAnno = if (mode == Mode.DAGGER) "@Inject constructor" else "constructor"
    val methodAnno = if (mode == Mode.SYNTHETIC) "@SyntheticAnno " else ""
    return buildString {
        append(classAnno)
        append("class ${name}_Bodies $ctorAnno() {\n")
        for (i in 0 until methodCount) {
            append(
                """
                |    ${methodAnno}fun m$i(input: List<String>, flag: Boolean): Map<String, Int> {
                |        val counts = input.map { s -> s.trim() }.filter { s -> s.isNotEmpty() }.groupingBy { s -> s }.eachCount()
                |        val scaled = counts.entries.associate { e -> e.key to e.value * $i }
                |        val total = scaled.values.fold(0) { acc, v -> acc + v }
                |        val label = when {
                |            total > 100 -> "high"
                |            total > 10 -> "mid"
                |            else -> "low"
                |        }
                |        val extra: Any = if (flag) label else total
                |        val bonus = if (extra is String) extra.length else 0
                |        var sum = 0
                |        for (idx in input.indices) {
                |            sum += idx + input[idx].length
                |        }
                |        val merged = scaled.toMutableMap()
                |        merged[label] = sum + bonus
                |        return merged
                |    }
                |
                """.trimMargin()
            )
        }
        append("}\n")
    }
}
