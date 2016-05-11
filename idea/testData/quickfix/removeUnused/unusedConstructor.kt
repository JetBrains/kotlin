// "Safe delete constructor" "true"
class Owner(val x: Int) {
    <caret>constructor(): this(42)
}