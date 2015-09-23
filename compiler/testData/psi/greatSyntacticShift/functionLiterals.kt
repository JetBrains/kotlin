class Foo
class Bar

fun a(vararg a : Any) = a

fun test() {
a(1
, {}
, { -> 1}
, {1}
, {x}
, {-> 1}
, {x -> 1}
, {x, y -> 1}
, {x -> 1}
, {(x)}
)
}
