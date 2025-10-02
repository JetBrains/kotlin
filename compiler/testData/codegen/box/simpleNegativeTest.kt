// IGNORE_BACKEND: JVM, NATIVE, WASM, JS

// It's a simple negative test to makes sure that underline running infra doing something and will fail when it's expected. 

fun box(): String {
    return "OK not to be OK"
}