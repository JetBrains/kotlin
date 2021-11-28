// ALLOW_AST_ACCESS

package test

annotation class Anno(
    val value: Array<Bnno>
)

annotation class Bnno(
    val value: String
)

@Anno(
    value = [Bnno("x"), Bnno("y")]
)
public class AnnotationInArray
