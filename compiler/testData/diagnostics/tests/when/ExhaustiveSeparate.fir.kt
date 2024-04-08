// RUN_PIPELINE_TILL: FRONTEND

fun booleanFoo(x: Boolean): String {
    if (x == false) return "hello"
    return when (x) {
        true -> "bye"
    }
}

enum class MyEnum {
    A, B, C
}

fun foo(x: MyEnum?): Int {
    return when (x) {
        null -> 0
        MyEnum.A -> 2
        MyEnum.B -> 2
        MyEnum.C -> 3
    }
}

fun foo(x: MyEnum): Int {
    if (x == MyEnum.A) return 1
    return when (x) {
        MyEnum.B -> 2
        MyEnum.C -> 3
    }
}

fun bar(x: MyEnum): Int {
    return <!NO_ELSE_IN_WHEN!>when<!> (x) {
        MyEnum.B -> 2
        MyEnum.C -> 3
    }
}

fun allGone1(e: MyEnum): Int {
    if (e == MyEnum.A) return 1
    if (e == MyEnum.B) return 2
    if (e == MyEnum.C) return 3

    return when (e) {
        <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> 0
    }
}

fun allGone2(e: MyEnum): Int {
    if (e == MyEnum.A) return 1
    if (e == MyEnum.B) return 2
    if (e == MyEnum.C) return 3

    val x = <!NO_ELSE_IN_WHEN!>when<!> (e) {

    }

    return 0
}

fun allGone3(e: MyEnum): Int {
    when (e) {
        MyEnum.A -> return 1
        MyEnum.B -> return 2
        MyEnum.C -> return 3
    }
    return <!NO_ELSE_IN_WHEN!>when<!> (e) {
        MyEnum.A -> return 1
    }
}

sealed interface Animal {
    data class Dog(val name: String): Animal
    data class Cat(val eatsMice: Boolean): Animal
    data object Unknown: Animal
}

fun baz(a: Animal): String {
    if (a == Animal.Unknown) return "unknown"
    return when (a) {
        is Animal.Dog -> "${a.name} the Dog"
        is Animal.Cat -> "Mr. Cat"
    }
}

fun magicAnimal(): Animal = Animal.Unknown

fun magicBaz(a: Animal): String {
    return when (magicAnimal()) {
        is Animal.Dog -> "The Dog"
        is Animal.Cat -> "Mr. Cat"
        is Animal.Unknown -> "Who knows?"
    }
}
