//ALLOW_AST_ACCESS
package test

inline fun a() {}

inline fun b() {}

inline fun c(crossinline f: () -> Unit) {
    object { init { f() }}
    { f() }
}

inline fun d() {
    c {}
}
