object Foo where T : G
object Foo : Bar where T : G

object Foo() where T : G
object Foo() : Bar where T : G

object Foo() : Bar where T : G {}