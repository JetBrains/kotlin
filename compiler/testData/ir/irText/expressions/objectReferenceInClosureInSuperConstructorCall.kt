// FIR_IDENTICAL

abstract class Base(val lambda: () -> Any)

object Test : Base({ -> Test })
