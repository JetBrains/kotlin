fun box(): String {
    class A {
        var result: String = "Fail";
        {
            result = "OK"
        }
    }

    return (::A)().result
}
