package test

fun checkEnum(e: Enum) = when(e) {
	Enum.SOUTH -> println(1)
	Enum.NORTH -> println(2)
	Enum.WEST -> println(3)
	Enum.EAST -> println(42)
}
