// FIR_IDENTICAL

// NO_SIGNATURE_DUMP
// ^KT-57428

abstract class Base(val lambda: () -> Any)

object Test : Base({ -> Test })
