// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_VARIABLE

fun example() {
    val a = if (true) true else false
    val b = if (true) else false
    val c = if (true) true
    val d = if (true) true else;
    val e = if (true) {} else false
    val f = if (true) true else {}

    <!UNRESOLVED_REFERENCE!>{
        if (true) true
    }()<!>;

    <!UNRESOLVED_REFERENCE!>{
        if (true) true else false
    }()<!>;

    <!UNRESOLVED_REFERENCE!>{
        if (true) {} else false
    }()<!>;


    <!UNRESOLVED_REFERENCE!>{
        if (true) true else {}
    }()<!>

    fun t(): Boolean {
        return if (true) true
    }

    return if (true) true else {}
}