// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_VARIABLE

fun test(d: dynamic) {
    d == null
    d != null
    d["foo"] == null
    d["foo"] != null
    d.foo == null
    d.foo != null
}