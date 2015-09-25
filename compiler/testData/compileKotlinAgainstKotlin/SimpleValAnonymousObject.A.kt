package pkg

interface ClassA {
    companion object {
        val DEFAULT = object : ClassA {
            override fun toString() = "OK"
        }
    }
}
