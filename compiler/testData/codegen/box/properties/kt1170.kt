// IGNORE_BACKEND_FIR: JVM_IR
public abstract class BaseClass() {
    open val kind : String = "BaseClass "

    fun getKindValue() : String {
        return kind
    }
}

public class Subclass : BaseClass() {
    override val kind : String = "Subclass "
}

fun box(): String {
    val r = Subclass().getKindValue() + Subclass().kind
    return if(r == "Subclass Subclass ") "OK" else "fail"
}
