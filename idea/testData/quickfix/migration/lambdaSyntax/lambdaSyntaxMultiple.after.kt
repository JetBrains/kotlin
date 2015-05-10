// "Migrate lambda syntax in whole project" "true"

val a = fun (): Int {

    val b = fun (): Int = 5

    return b()
}