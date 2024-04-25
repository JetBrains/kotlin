// CHECK_TYPE

fun test() {
    run f@{
        checkSubtype<Nothing>(return@f 1)
    }
}
