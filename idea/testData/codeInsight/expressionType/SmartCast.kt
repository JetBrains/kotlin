fun foo(x: Any) {
    if (x is String) {
        <caret>x.length
    }
}

// TYPE: if (x is String) {         x.length     } -> <html>kotlin.Unit</html>
// TYPE: x -> <html>kotlin.String (smart cast)</html>
// TYPE: x.length -> <html>kotlin.Int</html>
