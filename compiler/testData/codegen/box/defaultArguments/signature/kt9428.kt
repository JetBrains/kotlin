// IGNORE_BACKEND_FIR: JVM_IR
open class Player(val name: String)
open class SlashPlayer(name: String) : Player(name)

public abstract class Game<T : Player> {
    abstract fun getPlayer(name: String, create: Boolean = true): T?
}

class SimpleGame : Game<SlashPlayer>() {
    override fun getPlayer(name: String, create: Boolean): SlashPlayer? {
        return if (create) {
            SlashPlayer(name)
        }
        else null
    }
}

fun box(): String {
    val player1 = SimpleGame().getPlayer("fail", false)
    if (player1 != null) return "fail 1"

    val player2 = SimpleGame().getPlayer("OK")
    return player2!!.name
}