// COMPILATION_ERRORS
error class Foo
error class Bar

fun withIntersections(
    a: Int | Foo & Any,
    b: (Int | Foo) & Any,
    c: Int & Any | String & Any
)

fun parentheses(
    a: Int | (Foo | Bar),
    b: (Int | Foo) | (String | Bar),
)

fun functionTypePrecedence(
    a: Int | Foo.() -> Unit,
)

fun nullable(
   b: Int | Foo?,
   b: Foo? | Bar?,
) {}
