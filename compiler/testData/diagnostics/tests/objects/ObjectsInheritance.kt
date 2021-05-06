// FIR_IDENTICAL
package toplevelObjectDeclarations

object CObj {}

object DOjb : <!SINGLETON_IN_SUPERTYPE!>CObj<!> {}
