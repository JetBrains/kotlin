trait Trait {
    val member: String
}

class Klass: Trait {
    override val member: String = ":)"
}

fun main(args: Array<String>) {
    val t: Trait = Klass()
    println(t.member)
}
