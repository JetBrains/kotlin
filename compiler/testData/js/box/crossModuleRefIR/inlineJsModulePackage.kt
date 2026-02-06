// SPLIT_PER_MODULE
// MODULE: lib1
// FILE: lib1.js
define("lib1", [], function() {
    return {
        foo: function() {
            return "OK";
        }
    };
})

// MODULE: lib2(lib1)
// JS_MODULE_KIND: AMD
// FILE: lib2.kt
@file:JsModule("lib1")

external fun foo(): String

// MODULE: lib3(lib2)
// JS_MODULE_KIND: AMD
// FILE: lib3.kt
inline fun bar() = foo()

// MODULE: main(lib3)
// JS_MODULE_KIND: AMD
// FILE: main.kt

fun box() = bar()
