fun box(): String {
    Base<String>{}.call("")
    Derived{}.call("")
    return "OK"
}
