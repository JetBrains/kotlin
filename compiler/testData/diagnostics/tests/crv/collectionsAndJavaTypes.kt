// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// FULL_JDK

import java.util.Optional

fun list(m: MutableList<String>) {
    m.add("")
    m.isEmpty()
    m.toString()
}

fun javaList(m: ArrayList<String>) {
    m.add("x")
    m.isEmpty()
}

fun javaSet(s: LinkedHashSet<String>) {
    s.add("x")
    s.isEmpty()
    s.toString()
    s.first()
}

fun map(s: MutableMap<String, String>) {
    s.get("")
    s.getOrDefault("", "")
    s.put("", "")
    s.putIfAbsent("", "")
    s.merge("", "") { a, b -> a + b }
}

fun javaMap(s: LinkedHashMap<String, String>) {
    s.get("")
    s.getOrDefault("", "")
    s.put("", "")
    s.putIfAbsent("", "")
    s.merge("", "") { a, b -> a + b }
}

fun iterable(i: Iterable<String>) {
    i.iterator()
    i.spliterator()
}

fun collection(i: Collection<String>) {
    i.iterator()
    i.stream()
    i.parallelStream()
}

fun optional(s: String?) {
    Optional.of(s!!)
    Optional.empty<String>()
    Optional.ofNullable(s)
    val o = Optional.ofNullable(s)
    o.get()
}

fun stringBuilder(sb: StringBuilder, ss: CharSequence) {
    sb.append("")
    sb.length
    sb.get(0)
    ss.length
    ss.get(0)
}

/* GENERATED_FIR_TAGS: functionDeclaration, inProjection, lambdaLiteral, nullableType, outProjection, samConversion,
stringLiteral */
