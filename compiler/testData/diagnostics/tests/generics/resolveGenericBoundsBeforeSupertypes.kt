// Tests that generic bounds in the object supertype are resolved prior to the supertype itself

object O : Tr<V<*>>

trait Tr<T>

class V<out S>
