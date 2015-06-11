// Class constructor parameter type CAN be recursively annotated
annotation class RecursivelyAnnotated(val x: @RecursivelyAnnotated(1) Int)