package test

enum class Weapon {
    ROCK,
    PAPER,
    SCISSORS
}

annotation class JustEnum(val weapon: Weapon)

annotation class EnumArray(val enumArray: Array<Weapon>)

JustEnum(Weapon.SCISSORS)
EnumArray(array())
class C1

EnumArray(array(Weapon.PAPER, Weapon.ROCK))
class C2
