interface WorldObject {
    val name: String
}

fun testB(worldObj: WorldObject) {
    val y = worldObj.let {
        println("object name: ${it.name}")
        it
    }
}

// 0 CHECKCAST