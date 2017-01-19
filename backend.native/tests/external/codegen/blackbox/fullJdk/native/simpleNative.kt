// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// FULL_JDK

package foo

class WithNative {
    external fun foo()
}

fun box(): String {
    try {
        WithNative().foo()
        return "Link error expected"
    }
    catch (e: java.lang.UnsatisfiedLinkError) {
        return "OK"
    }
}
