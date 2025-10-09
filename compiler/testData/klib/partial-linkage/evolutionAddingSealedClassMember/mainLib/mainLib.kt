fun lib(): String {
    val x = last()
    return when(x) {
        is Y -> "fail 1"
        is Z -> "fail 2"

        else -> {
            if (x.name == "W")
                "OK"
            else
                "fail 3"
        }
    }
}

