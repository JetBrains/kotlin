fun foo(x: Any) {
    if (x is String) {
        <caret>x.length
    }
}

// TYPE: if (x is String) {         x.length     } -> <html>Unit</html>
// TYPE: x -> <html>String (smart cast)</html>
// TYPE: x.length -> <html>Int</html>
