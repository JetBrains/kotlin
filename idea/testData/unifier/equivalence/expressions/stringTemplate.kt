fun foo(n: Int, f: (Int) -> Int) {
    <selection>"test: $n, ${f(n)}"</selection>
    "test: ${n}, ${f(n)}"
    "test: ${f(n)}, ${n}"
    "test: n, ${f(n)}"
    "test: $n, $f"
}