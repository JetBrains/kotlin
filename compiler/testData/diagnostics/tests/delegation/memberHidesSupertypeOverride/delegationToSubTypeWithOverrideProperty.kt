public interface Base {
    var test: String
        get() = "OK"
        set(<!UNUSED_PARAMETER!>s<!>: String) {
        }
}

public interface Base2 : Base {
    override var test: String
            get() = "OK2"
            set(<!UNUSED_PARAMETER!>value<!>) {}
}

class Delegate : Base2 {

}

fun box(): String {
    object : Base, Base2 by Delegate() {
    }

    <!DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE!>object<!> : Base2, Base by Delegate() {
    }

    return "OK"
}
