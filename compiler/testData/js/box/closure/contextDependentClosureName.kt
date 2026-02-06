fun foo(a: () -> Unit) = a()

// CHECK_CALLED_IN_SCOPE: scope=box function=box$lambda
fun box(): String {
    var result = "FAILURE"
    foo { result = "OK" }
    return result
}
