// Class constructor parameter CAN be recursively annotated
class RecursivelyAnnotated(@RecursivelyAnnotated(1) val x: Int)