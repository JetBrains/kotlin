// TARGET_BACKEND: JS_IR

// FILE: a.kt
@JsExport
fun exclamate(s: String) =
    "$s!"

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@JsFun("""
    function (a, b) {
        return _.exclamate(a) +
            _.exclamate(b);
    }
""")
external fun exclamateAndConcat(`a!`: String, `b!`: String): String

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



fun box() {

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

    js("""
    function localFun(hello, world) {
        return '' + hello + ', ' + world + '!';       
    }
    """)

    js("localFun('hello', 'world')")
}

// EXPECTATIONS
// test.kt:41 box:
// test.kt:42 box: a!="hello":kotlin.String
// a.kt:11 box: a!="hello":kotlin.String, b!="world":kotlin.String
// a.kt:6 exclamate: s="hello":kotlin.String
// a.kt:12 box: a!="hello":kotlin.String, b!="world":kotlin.String
// a.kt:6 exclamate: s="world":kotlin.String
// test.kt:43 box: a!="hello":kotlin.String, b!="world":kotlin.String
// test.kt:45 box: a!="hello":kotlin.String, b!="world":kotlin.String, jesse="Jesse":kotlin.String
// a.kt:6 exclamate: s="Jesse":kotlin.String
// test.kt:47 box: a!="hello":kotlin.String, b!="world":kotlin.String, jesse="Jesse":kotlin.String
// a.kt:6 exclamate: s="Walter":kotlin.String
// test.kt:49 box: a!="hello":kotlin.String, b!="world":kotlin.String, jesse="Jesse":kotlin.String
// a.kt:6 exclamate: s="Walter":kotlin.String
// test.kt:49 box: a!="hello":kotlin.String, b!="world":kotlin.String, jesse="Jesse":kotlin.String, walter1="Walter!":kotlin.String
// a.kt:6 exclamate: s="Walter!":kotlin.String
// test.kt:52 box: a!="hello":kotlin.String, b!="world":kotlin.String, jesse="Jesse":kotlin.String, walter1="Walter!":kotlin.String
// a.kt:6 exclamate: s="Jesse":kotlin.String
// test.kt:52 box: a!="hello":kotlin.String, b!="world":kotlin.String, jesse="Jesse":kotlin.String, walter1="Walter!":kotlin.String, jesse1="Jesse!":kotlin.String
// a.kt:6 exclamate: s="Jesse!":kotlin.String
// a.kt:29 box: a!="hello":kotlin.String, b!="world":kotlin.String, jesse="Jesse":kotlin.String, walter1="Walter!":kotlin.String, jesse1="Jesse!":kotlin.String
// a.kt:22 value:
// test.kt:63 box: a!="hello":kotlin.String, b!="world":kotlin.String, jesse="Jesse":kotlin.String, walter1="Walter!":kotlin.String, jesse1="Jesse!":kotlin.String
// test.kt:59 localFun: hello="hello":kotlin.String, world="world":kotlin.String
// test.kt:64 box: a!="hello":kotlin.String, b!="world":kotlin.String, jesse="Jesse":kotlin.String, walter1="Walter!":kotlin.String, jesse1="Jesse!":kotlin.String
