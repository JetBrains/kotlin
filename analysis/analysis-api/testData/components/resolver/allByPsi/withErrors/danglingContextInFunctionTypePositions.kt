@Target(AnnotationTarget.TYPE)
annotation class Anno(val value: String = "")

@Target(AnnotationTarget.TYPE)
annotation class Other

val annotated: @Anno context(Int)
val annotatedWithType: @Anno context(Int) String

val lambda = { p: context(Int) -> }
val anonymousFunction = fun(p: context(Int)) {}

fun withVararg(vararg p: context(Int)) {}
fun withVarargType(vararg p: context(Int) String) {}
fun withAnnotatedVararg(vararg p: @Anno context(Int)) {}
fun withAnnotatedVarargType(vararg p: @Anno context(@Other Int) String) {}
fun withVarargReceiver(vararg p: context(Int) (String).) {}
fun withNullableVararg(vararg p: (@Anno context(Int) String)?) {}

data class Data(val p: context(Int))

interface Forms {
    val simple: @Anno context(Int)
    val withArguments: @Anno("1" + "2") context(Int)
    val several: @Anno @Other context(Int)
    val bracketed: @[Anno Other] context(Int)
    val useSiteTarget: @receiver:Anno context(Int)
    val suspendAfter: @Anno suspend context(Int)
    val suspendBefore: suspend @Anno context(Int)
}

interface InsideList {
    val inside: context(@Anno Int)
    val insideUnresolved: context(@Anno Unresolved)
    val bothSides: @Anno context(@Other Int, @Anno("x") String)
    val named: @Anno context(a: @Other Int)
}

interface TypeAfterList {
    val type: @Anno context(Int) String
    val unresolvedType: @Anno context(Int) Unresolved
    val nullableType: @Anno context(Int) String?
}

interface NullableAndParenthesized {
    val nullable: (@Anno context(Int))?
    val nullableWithoutAnnotation: (context(Int))?
    val nullableWithType: (@Anno context(Int) String)?
    val nullableWithReceiver: (@Anno context(Int) (String).)?
    val doubleNullable: ((@Anno context(Int))?)?
    val nullableInsideDangling: @Anno context(Int) (@Other context(Long))?
    val nullableWithoutType: ()?
    val nullableAnnotatedEntry: (context(@Other Int))?
    val nullableNullableType: (context(Int) String?)?
    val nullableSuspend: (suspend context(Int))?
    val suspendInsideDangling: context(Int) (suspend context(Long))?
    val suspendOutsideDangling: suspend context(Int) (context(Long))?
    val parenthesized: (@Anno context(Int))
}

interface Nested {
    val typeArgument: List<@Anno context(Int)>
    val functionTypeParameter: (@Anno context(Int)) -> Unit
    val functionTypeReturn: () -> @Anno context(Int)
}

fun (@Anno context(Int)).receiver() {}

fun parameter(p: @Anno context(Int)) {}

fun returnType(): @Anno context(Int) = TODO()

fun <T : @Anno context(Int)> bound() {}

fun <T> whereConstraint() where T : @Anno context(Int) {}

typealias Alias = @Anno context(Int)

interface Supertype : @Anno context(Int) {}

data class AnnotatedData(val p: @Anno context(Int))

class Setter {
    var p: Int = 0
        set(value: @Anno context(Int)) {}
}

fun body(a: Any) {
    val local: @Anno context(Int) = TODO()
    val lambda = { p: @Anno context(Int) -> }
    val anonymousFunction = fun(p: @Anno context(Int)) {}
    val asTarget = (a as @Anno context(Int))
    val isTarget = (a is @Anno context(Int))
    try {} catch (e: @Anno context(Int)) {}
}
