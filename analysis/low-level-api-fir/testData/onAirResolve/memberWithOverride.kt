interface Base {
    fun baseMember(): Int
}

class Derived : Base {
    /*PLACE*/fun someMember() = "Hello"
}

/*ONAIR*/override fun baseMember() = 3