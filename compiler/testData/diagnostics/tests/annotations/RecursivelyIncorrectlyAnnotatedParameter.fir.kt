// Class constructor parameter CAN be recursively annotated
class RecursivelyAnnotated(@<!NOT_AN_ANNOTATION_CLASS!>RecursivelyAnnotated<!>(1) val x: Int)
