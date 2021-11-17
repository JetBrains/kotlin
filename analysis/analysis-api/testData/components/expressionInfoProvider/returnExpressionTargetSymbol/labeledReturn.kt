fun test() {
    run {
        return@run/* <anonymous>@(2,9) */
    }
    run {
        return@test/* test@(1,1) */
    }
    run a@{
        return@a/* <anonymous>@(8,11) */
    }
    run {
        with("") {
            return@with/* <anonymous>@(12,18) */
            return@run/* <anonymous>@(11,9) */
            return@test/* test@(1,1) */
        }
    }
}

val i = a@{
    return@a/* <anonymous>@(20,11) */
}
