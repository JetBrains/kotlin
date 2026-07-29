// TARGET_BACKEND: WASM
package foo.test

class NonLocal
val nonLocalObject = object {}

fun box(): String {

    class Local
    var s = Local::class.toString()
    if (s != "class foo.test.Local") return "Fail 1. Got '$s'"

    val localObject = object {}
    s = localObject::class.toString()
    if (s != "class foo.test.<no name provided>") return "Fail 2. Got '$s'"

    s = NonLocal::class.toString()
    if (s != "class foo.test.NonLocal") return "Fail 3. Got '$s'"

    s = nonLocalObject::class.toString()
    if (s != "class foo.test.<no name provided>") return "Fail 4. Got '$s'"

    return "OK"
}
