// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// FULL_JDK

import java.util.Optional

fun list(m: MutableList<String>) {
    m.add("")
    m.<!RETURN_VALUE_NOT_USED!>isEmpty<!>()
    m.<!RETURN_VALUE_NOT_USED!>toString<!>()
}

fun javaList(m: ArrayList<String>) {
    m.add("x")
    m.<!RETURN_VALUE_NOT_USED!>isEmpty<!>()
}

fun javaSet(s: LinkedHashSet<String>) {
    s.add("x")
    s.<!RETURN_VALUE_NOT_USED!>isEmpty<!>()
    s.<!RETURN_VALUE_NOT_USED!>toString<!>()
    s.<!RETURN_VALUE_NOT_USED!>first<!>()
}

fun map(s: MutableMap<String, String>) {
    s.<!RETURN_VALUE_NOT_USED!>get<!>("")
    s.<!RETURN_VALUE_NOT_USED!>getOrDefault<!>("", "")
    s.put("", "")
    s.putIfAbsent("", "")
    s.merge("", "") { a, b -> a + b }
}

fun javaMap(s: LinkedHashMap<String, String>) {
    s.<!RETURN_VALUE_NOT_USED!>get<!>("")
    s.<!RETURN_VALUE_NOT_USED!>getOrDefault<!>("", "")
    s.put("", "")
    s.putIfAbsent("", "")
    s.merge("", "") { a, b -> a + b }
}

fun iterable(i: Iterable<String>) {
    i.<!RETURN_VALUE_NOT_USED!>iterator<!>()
    i.<!RETURN_VALUE_NOT_USED!>spliterator<!>()
}

fun collection(i: Collection<String>) {
    i.<!RETURN_VALUE_NOT_USED!>iterator<!>()
    i.<!RETURN_VALUE_NOT_USED!>stream<!>()
    i.<!RETURN_VALUE_NOT_USED!>parallelStream<!>()
}

fun optional(s: String?) {
    Optional.<!RETURN_VALUE_NOT_USED!>of<!>(s!!)
    Optional.<!RETURN_VALUE_NOT_USED!>empty<!><String>()
    Optional.<!RETURN_VALUE_NOT_USED!>ofNullable<!>(s)
    val o = Optional.ofNullable(s)
    o.<!RETURN_VALUE_NOT_USED!>get<!>()
}

fun stringBuilder(sb: StringBuilder, ss: CharSequence) {
    sb.append("")
    sb.<!RETURN_VALUE_NOT_USED!>length<!>
    sb.<!RETURN_VALUE_NOT_USED!>get<!>(0)
    ss.<!RETURN_VALUE_NOT_USED!>length<!>
    ss.<!RETURN_VALUE_NOT_USED!>get<!>(0)
}

/* GENERATED_FIR_TAGS: functionDeclaration, inProjection, lambdaLiteral, nullableType, outProjection, samConversion,
stringLiteral */
