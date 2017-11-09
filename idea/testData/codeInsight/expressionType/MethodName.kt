fun foo(value: String) {
    print<caret>ln(value)
}

// TYPE: println(value) -> <html>Unit</html>
