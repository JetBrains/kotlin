// TARGET_BACKEND: JS_IR

// FILE: a.kt
@JsExport
fun exclamate(a: String) =
    "$a!"

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@JsFun("""
    function (a, b) {
        return _.exclamate(a) +
            _.exclamate(b);
    }
""")
external fun exclamateAndConcat(a: String, b: String): String

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@JsPolyfill("""
if (typeof String.prototype.myAwesomePolyfill === "undefined") {
    Object.defineProperty(String.prototype, "myAwesomePolyfill", {
        value: function () {
            return this + "!";
        }
    });
}
""")
internal inline fun String.myAwesomePolyfill(): Boolean =
    asDynamic()
        .myAwesomePolyfill()

val walter1VarDecl = "var walter1 = _.exclamate('Walter');"
val jesse1VarDecl = "var jesse1 = _.exclamate(jesse);"

// FILE: test.kt

fun noop() {}

fun box() {
    noop()
    exclamateAndConcat(
        "hello",
        "world")
    val jesse = "Jesse"
    js(
        "_.exclamate(jesse);") // Local variable is captured
    js(
        "_.exclamate('Walter');") // No local variables captured
    js(
        walter1VarDecl +
        "_.exclamate(walter1);")
    js(
        jesse1VarDecl +
                "_.exclamate(jesse1);")
    
    "foo".myAwesomePolyfill()
}

// EXPECTATIONS
// test.kt:39 box
// test.kt:36 noop
// test.kt:41 box
// test.kt:42 box
// a.kt:11 box
// a.kt:6 exclamate
// a.kt:12 box
// a.kt:6 exclamate
// test.kt:43 box
// test.kt:45 box
// a.kt:6 exclamate
// test.kt:47 box
// a.kt:6 exclamate
// test.kt:49 box
// a.kt:6 exclamate
// test.kt:49 box
// a.kt:6 exclamate
// test.kt:52 box
// a.kt:6 exclamate
// test.kt:52 box
// a.kt:6 exclamate
// a.kt:29 box
// a.kt:22 value
// test.kt:56 box