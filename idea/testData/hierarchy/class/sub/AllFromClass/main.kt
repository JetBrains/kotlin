open class <caret>A

open class B: A()

trait T: A

open class C: B()

trait U: T

object O1: A()

object O2: B()

object O3: C()

object O4: T

object O5: U
