// Class constructor parameter CAN be recursively annotated
annotation class RecursivelyAnnotated(@RecursivelyAnnotated(1) val x: Int)