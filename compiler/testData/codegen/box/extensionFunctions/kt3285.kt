// IGNORE_BACKEND_FIR: JVM_IR
var sayResult = ""

class NoiseMaker {
    fun say(str: String) { sayResult += str }
}

fun noiseMaker(f: NoiseMaker.() -> Unit) {
    val noiseMaker = NoiseMaker()
    noiseMaker.f()
}

abstract class Pet {
    fun <T> NoiseMaker.playWith(friend: T) {
        say("Playing with " + friend)
    }

    abstract fun play(): Unit
}

class Doggy(): Pet()  {
    override fun play() = noiseMaker {
        say("Time to play! ")
        playWith("my owner!")
    }
}

fun box(): String {
    Doggy().play()
    if (sayResult != "Time to play! Playing with my owner!") return "fail: $sayResult"

    return "OK"
}