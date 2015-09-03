package bar

@file:foo
val prop

@file:[bar baz]
fun func() {}

@file:[baz]
class C

@file:
trait T

@file:[]
trait T