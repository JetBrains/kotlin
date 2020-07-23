// Class constructor parameter CAN be recursively annotated
class RecursivelyAnnotated(<!UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE!>@RecursivelyAnnotated(1)<!> val x: Int)
