fun foo(x: Any) {
    if (x is String) {
        <caret>x.length
    }
}

// TYPE: x -> <html>String (smart cast from Any)</html>
// TYPE: x.length -> <html>Int</html>
