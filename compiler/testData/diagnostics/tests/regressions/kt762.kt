//KT-762 Wrong highlighting in imports (No errors reported on unresolved imports)
import <!UNRESOLVED_REFERENCE!>aaa<!> // must be an error

fun main(args : Array<String>) {}
