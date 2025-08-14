// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// FULL_JDK

import java.util.Optional

fun list(m: MutableList<String>) {
    m.add("")
    <!RETURN_VALUE_NOT_USED!>m.isEmpty()<!>
    <!RETURN_VALUE_NOT_USED!>m.toString()<!>
}

fun javaList(m: ArrayList<String>) {
    m.add("x")
    <!RETURN_VALUE_NOT_USED!>m.isEmpty()<!>
}

fun javaSet(s: LinkedHashSet<String>) {
    s.add("x")
    <!RETURN_VALUE_NOT_USED!>s.isEmpty()<!>
    <!RETURN_VALUE_NOT_USED!>s.toString()<!>
    <!RETURN_VALUE_NOT_USED!>s.first()<!>
}

fun map(s: MutableMap<String, String>) {
    <!RETURN_VALUE_NOT_USED!>s.get("")<!>
    <!RETURN_VALUE_NOT_USED!>s.getOrDefault("", "")<!>
    s.put("", "")
    s.putIfAbsent("", "")
    s.merge("", "") { a, b -> a + b }
}

fun javaMap(s: LinkedHashMap<String, String>) {
    <!RETURN_VALUE_NOT_USED!>s.get("")<!>
    <!RETURN_VALUE_NOT_USED!>s.getOrDefault("", "")<!>
    s.put("", "")
    s.putIfAbsent("", "")
    s.merge("", "") { a, b -> a + b }
}

fun iterable(i: Iterable<String>) {
    <!RETURN_VALUE_NOT_USED!>i.iterator()<!>
    <!RETURN_VALUE_NOT_USED!>i.spliterator()<!>
}

fun collection(i: Collection<String>) {
    <!RETURN_VALUE_NOT_USED!>i.iterator()<!>
    <!RETURN_VALUE_NOT_USED!>i.stream()<!>
    <!RETURN_VALUE_NOT_USED!>i.parallelStream()<!>
}

fun optional(s: String?) {
    <!RETURN_VALUE_NOT_USED!>Optional.of(s!!)<!>
    <!RETURN_VALUE_NOT_USED!>Optional.empty<String>()<!>
    <!RETURN_VALUE_NOT_USED!>Optional.ofNullable(s)<!>
    val o = Optional.ofNullable(s)
    <!RETURN_VALUE_NOT_USED!>o.get()<!>
}

fun stringBuilder(sb: StringBuilder, ss: CharSequence) {
    sb.append("")
    sb.length // .length is not reported because of KT-80179
    <!RETURN_VALUE_NOT_USED!>sb.get(0)<!>
    <!RETURN_VALUE_NOT_USED!>ss.length<!>
    <!RETURN_VALUE_NOT_USED!>ss.get(0)<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, inProjection, lambdaLiteral, nullableType, outProjection, samConversion,
stringLiteral */
