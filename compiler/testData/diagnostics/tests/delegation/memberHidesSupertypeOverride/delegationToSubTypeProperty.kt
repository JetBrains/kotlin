public interface Base {
    val test: String
            get() = "OK"
}

public interface Base2 : Base 

class Delegate : Base2

fun box(): String {
    object : Base, Base2 by Delegate() {

    }

    object : Base2, Base by Delegate() {
    
    }

    return "OK"
}
