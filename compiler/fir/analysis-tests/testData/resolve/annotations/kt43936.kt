// ISSUE: KT-43936
// WITH_STDLIB

import FooOperation.*

interface Operation<T>

class FooOperation(val foo: String) : Operation<Boom> {

    @Suppress("test")
    class Boom(val bar: String)
}
