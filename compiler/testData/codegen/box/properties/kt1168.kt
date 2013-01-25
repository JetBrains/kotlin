public abstract class BaseClass() {
    protected abstract val kind : String

    protected open val kind2 : String = " kind1"

    fun debug() = kind + kind2
}

public class Subclass : BaseClass() {
    override val kind : String = "Physical"

    override val kind2 : String = " kind2"
}

fun box():String = if(Subclass().debug() == "Physical kind2") "OK" else "fail"
