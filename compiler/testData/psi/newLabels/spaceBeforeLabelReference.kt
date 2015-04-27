fun foo() {
    break @l1

    val x = 1

    return @l2 1

    val x = 2

    return @l3

    val x = 3

    continue @l4 5

    val x = 6

    break/**/@l5

    val x = 7

    return /**/@l6

    val x = 8

    return//
@l7 4

    val x = 9
}