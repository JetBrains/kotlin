var global: Int = 0
    get() {
        println("Get global = $field")
        return field
    }
    set(value) {
        println("Set global = $value")
        field = value
    }

class Test {
    var member: Int = 0
        get() {
            println("Get member = $field")
            return field
        }
        set(value) {
            println("Set member = $value")
            field = value
        }
}

fun main(args:Array<String>) {
    global = 1

    val test = Test()
    test.member = 42

    global = test.member
    test.member = global
}