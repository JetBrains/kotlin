// "Migrate lambda syntax in whole project" "true"

val a = { <caret>(): Int ->

    val b = { (): Int -> 5 }

    b()
}